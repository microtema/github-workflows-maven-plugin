package de.microtema.maven.plugin.github.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import de.microtema.maven.plugin.github.workflow.job.*;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import de.microtema.model.converter.util.ClassUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class PipelineGeneratorMojo extends AbstractMojo {

    String workflowFilePostFixName = "-workflow.yaml";

    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    String githubWorkflowsDir = ".github/workflows";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "variables")
    LinkedHashMap<String, String> variables = new LinkedHashMap<>();

    @Parameter(property = "stages")
    LinkedHashMap<String, String> stages = new LinkedHashMap<>();

    @Parameter(property = "clusters")
    LinkedHashMap<String, String> clusters = new LinkedHashMap<>();

    @Parameter(property = "service-url")
    String serviceUrl;

    @Parameter(property = "runs-on")
    String runsOn;

    List<TemplateStageService> templateStageServices = new ArrayList<>();

    LinkedHashMap<String, String> defaultVariables = new LinkedHashMap<>();

    public void execute() {

        String appName = Optional.ofNullable(project.getName()).orElse(project.getArtifactId());

        // Skip maven sub modules
        if (!PipelineGeneratorUtil.isGitRepo(project)) {

            logMessage("Skip maven module: " + appName + " since it is not a git repo!");

            return;
        }

        String rootPath = PipelineGeneratorUtil.getRootPath(project);

        File rootDir = new File(rootPath, githubWorkflowsDir);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        List<MetaData> workflows = getWorkflowFiles();

        cleanupWorkflows(rootDir, workflows);

        defaultVariables.put("GITHUB_TOKEN", "${{ secrets.GITHUB_TOKEN }}");
        defaultVariables.put("APP_NAME", project.getArtifactId());
        defaultVariables.put("VERSION", project.getVersion());

        templateStageServices.add(ClassUtil.createInstance(VersioningTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(CompileTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(SecurityTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(UnitTestTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(IntegrationTestTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(SonarTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(BuildTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PackageTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(TagTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PublishTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DbMigrationTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PromoteTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DeploymentTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(HelmTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(ReadynessTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(RegressionTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PerformanceTemplateStageService.class));


        if (PipelineGeneratorUtil.hasSonarProperties(project)) {

            String sonarToken = PipelineGeneratorUtil.getProperty(project, "sonar.login", "${{ secrets.SONAR_TOKEN }}");

            defaultVariables.put("SONAR_TOKEN", sonarToken);
        }

        if (!defaultVariables.containsKey("JAVA_VERSION")) {

            String javaVersion = PipelineGeneratorUtil.getProperty(project, "maven.compiler.target.version", PipelineGeneratorUtil.getProperty(project, "java.version", "17.x"));

            defaultVariables.put("JAVA_VERSION", javaVersion);
        }

        String mavenCliOptions = "--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true";

        if (PipelineGeneratorUtil.existsMavenSettings(project)) {
            mavenCliOptions = "-s settings.xml " + mavenCliOptions;
        }

        defaultVariables.put("MAVEN_CLI_OPTS", mavenCliOptions);

        defaultVariables.forEach((key, value) -> variables.putIfAbsent(key, value));

        for (MetaData metaData : workflows) {
            executeImpl(appName, metaData);
        }
    }

    private void cleanupWorkflows(File rootDir, List<MetaData> workflows) {

        File[] files = rootDir.listFiles((dir, name) -> StringUtils.contains(name, workflowFilePostFixName));

        if (Objects.isNull(files) || files.length == 0) {
            return;
        }

        logMessage("Found (" + files.length + ") workflow(s) in -> " + rootDir.getPath());

        Set<String> fileNames = workflows.stream().map(it -> it.getBranchName() + workflowFilePostFixName).collect(Collectors.toSet());

        Stream.of(files).filter(it -> !fileNames.contains(it.getName())).forEach(this::removeWorkflow);
    }

    private void removeWorkflow(File file) {

        file.delete();

        logMessage("Remove " + file.getName() + " since it is not required anymore!");
    }

    List<MetaData> getWorkflowFiles() {

        List<MetaData> workflows = new ArrayList<>();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();
            String[] branches = StringUtils.split(stage.getValue(), ",");

            for (String branchPattern : branches) {

                String branchName = branchPattern.replaceAll("[^a-zA-Z0-9]", StringUtils.EMPTY);

                MetaData metaData = new MetaData();

                metaData.setStageName(stageName);
                metaData.setBranchName(branchName);
                metaData.setBranchPattern(branchPattern);

                workflows.add(metaData);
            }
        }

        return workflows;
    }

    void executeImpl(String appName, MetaData metaData) {

        String rootPath = PipelineGeneratorUtil.getRootPath(project);

        File dir = new File(rootPath, githubWorkflowsDir);

        String version = project.getVersion();

        switch (metaData.getBranchName()) {
            case "feature":
            case "develop":
                break;
            case "release":
                version = version.replace("-SNAPSHOT", "-RC");
                break;
            case "hotfix":
                version = version.replace("-SNAPSHOT", "-FIX");
                break;
            case "master":
                version = version.replace("-SNAPSHOT", "");
                version = version.replace("-RC", "");
                version = version.replace("-FIX", "");
                break;
        }

        variables.put("VERSION", version);

        String stageName = metaData.getStageName();

        variables.put("STAGE_NAME", stageName);
        variables.entrySet().forEach(it -> it.setValue(it.getValue()
                .replace("$STAGE_NAME", stageName.toUpperCase())
                .replace("$stage_name", stageName.toLowerCase())));

        variables.entrySet().stream().filter(it -> StringUtils.startsWith(it.getValue(), "secrets.")).forEach(it -> it.setValue("${{ " + it.getValue() + " }}"));

        String pipeline = PipelineGeneratorUtil.getTemplate("pipeline");

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", appName + " [" + stageName.toUpperCase() + "]")
                .replace("%BRANCH_NAME%", metaData.getBranchPattern())
                .replace("  %ENV%", getVariablesTemplate())
                .replace("  %JOBS%", getStagesTemplate(metaData));

        File githubWorkflow = new File(dir, metaData.getBranchName() + workflowFilePostFixName);

        logMessage("Generate Github Workflows Pipeline for " + appName + " -> " + githubWorkflow.getName());

        if (PipelineGeneratorUtil.hasMavenWrapper(project)) {
            pipeline = pipeline.replaceAll("mvn ", "./mvnw ");
        }

        runsOn = Optional.ofNullable(runsOn).orElse("ubuntu-latest");

        boolean supportVersionJob = Stream.of("develop", "feature").noneMatch(it -> StringUtils.equalsIgnoreCase(it, metaData.getBranchName()));

        pipeline = pipeline
                .replaceAll("%RUNS_ON%", String.join(", ", runsOn.split(",")))
                .replaceAll("%POM_ARTIFACT%", String.valueOf(supportVersionJob));

        try (PrintWriter out = new PrintWriter(githubWorkflow)) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    String getVariablesTemplate() {

        variables.entrySet().forEach(it -> it.setValue(unMask(it.getValue())));

        String template = null;
        try {
            template = objectMapper.writeValueAsString(Collections.singletonMap("env", variables));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    String getStagesTemplate(MetaData metaData) {

        return templateStageServices.stream()
                .map(it -> it.getTemplate(this, metaData))
                .filter(Objects::nonNull)
                .map(it -> PipelineGeneratorUtil.trimEmptyLines(it, 2))
                .collect(Collectors.joining("\n\n"));
    }

    String unMask(String value) {

        if (StringUtils.isEmpty(value)) {
            return StringUtils.EMPTY;
        }

        return value.replaceAll("\"", "");
    }

    void logMessage(String message) {

        Log log = getLog();

        log.info("+----------------------------------+");
        log.info(message);
        log.info("+----------------------------------+");
    }

    public Map<String, String> getStages() {

        return stages;
    }

    public Map<String, String> getClusters() {

        return clusters;
    }

    public String getServiceUrl() {

        return serviceUrl;
    }

    public MavenProject getProject() {

        return project;
    }
}
