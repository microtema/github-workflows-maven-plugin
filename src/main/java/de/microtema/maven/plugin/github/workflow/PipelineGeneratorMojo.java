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
    Map<String, String> variables = new LinkedHashMap<>();

    @Parameter(property = "stages")
    Map<String, String> stages = new LinkedHashMap<>();

    @Parameter(property = "down-streams")
    Map<String, String> downStreams = new LinkedHashMap<>();

    @Parameter(property = "runs-on")
    String runsOn;

    @Parameter(property = "code-paths")
    String codePaths;

    private String appName;

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    private final LinkedHashMap<String, String> defaultVariables = new LinkedHashMap<>();

    @Parameter(property = "env-folder")
    String envFolder = ".github/env";

    public void execute() {

        appName = Optional.ofNullable(project.getName()).orElse(project.getArtifactId());

        // Skip maven sub modules
        if (!PipelineGeneratorUtil.isGitRepo(project)) {

            logMessage("Skip maven module: " + appName + " since it is not a git repo!");

            return;
        }

        injectTemplateStageServices();

        File rootDir = getOrCreateWorkflowsDir();

        List<MetaData> workflows = getWorkflowFiles();

        cleanupWorkflows(rootDir);

        applyDefaultVariables();

        for (MetaData metaData : workflows) {
            executeImpl(metaData, workflows);
        }
    }

    void injectTemplateStageServices() {
        templateStageServices.add(ClassUtil.createInstance(InitializeTemplateStageService.class));
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
        templateStageServices.add(ClassUtil.createInstance(ReadinessTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(SystemTestTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PerformanceTestTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DownstreamTemplateStageService.class));
    }

    void applyDefaultVariables() {

        defaultVariables.put("APP_NAME", project.getArtifactId());

        if (PipelineGeneratorUtil.isDeploymentRepo(project)) {
            return;
        }

        defaultVariables.put("GITHUB_TOKEN", "${{ secrets.GITHUB_TOKEN }}");

        if (PipelineGeneratorUtil.hasSonarProperties(project)) {

            String sonarToken = PipelineGeneratorUtil.getProperty(project, "sonar.login", "${{ secrets.SONAR_TOKEN }}");

            defaultVariables.put("SONAR_TOKEN", sonarToken);
        }

        if (!defaultVariables.containsKey("JAVA_VERSION")) {

            String javaVersion = PipelineGeneratorUtil.getProperty(project, "maven.compiler.target.version", PipelineGeneratorUtil.getProperty(project, "java.version", "17.x"));

            defaultVariables.put("JAVA_VERSION", javaVersion);
        }

        if (PipelineGeneratorUtil.isMicroserviceRepo(project) || !downStreams.isEmpty()) {

            String variableValue = variables.getOrDefault("REPO_ACCESS_TOKEN", "${{ secrets.REPO_ACCESS_TOKEN }}");

            variableValue = wrapSecretVariable(variableValue);

            variables.put("REPO_ACCESS_TOKEN", variableValue);

            variableValue = variables.getOrDefault("DEPLOYMENT_REPOSITORY", "${{ github.repository }}-deployments");

            variableValue = wrapSecretVariable(variableValue);

            variables.put("DEPLOYMENT_REPOSITORY", variableValue);
        }

        String mavenCliOptions = "--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true";

        if (PipelineGeneratorUtil.existsMavenSettings(project)) {
            mavenCliOptions = "-s settings.xml " + mavenCliOptions;
        }

        defaultVariables.put("MAVEN_CLI_OPTS", mavenCliOptions);

        String codePaths = ".github/** src/** pom.xml";

        if (PipelineGeneratorUtil.existsDockerfile(project)) {
            codePaths += " Dockerfile";
        }

        codePaths = variables.getOrDefault("CODE_PATHS", codePaths);

        defaultVariables.put("CODE_PATHS", codePaths);
    }

    private String wrapSecretVariable(String variableValue) {

        if (variableValue.startsWith("secrets.")) {

            return "${{ " + variableValue + " }}";
        }

        return variableValue;
    }

    File getOrCreateWorkflowsDir() {

        String rootPath = PipelineGeneratorUtil.getRootPath(project);

        File rootDir = new File(rootPath, githubWorkflowsDir);
        if (!rootDir.exists()) {

            boolean mkdirs = rootDir.mkdirs();

            if (mkdirs) {
                logMessage("Create .github directories");
            }
        }
        return rootDir;
    }

    void cleanupWorkflows(File rootDir) {

        File[] files = rootDir.listFiles((dir, name) -> StringUtils.contains(name, workflowFilePostFixName));

        if (Objects.isNull(files) || files.length == 0) {
            return;
        }

        Stream.of(files).forEach(it -> logMessage("Delete " + it.getName() + " workflow -> " + it.delete()));
    }

    String getWorkflowFileName(MetaData metaData, List<MetaData> workflows) {

        String branchName = metaData.getBranchName();

        boolean duplicate = workflows.stream().filter(it -> StringUtils.equalsIgnoreCase(it.getBranchName(), branchName)).count() > 1;

        String workflowName = branchName + workflowFilePostFixName;

        if (duplicate) {
            workflowName = branchName + "-" + metaData.getStageName() + workflowFilePostFixName;
        }

        return workflowName;
    }

    List<MetaData> getWorkflowFiles() {

        List<MetaData> workflows = new ArrayList<>();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();
            String[] branches = StringUtils.split(stage.getValue(), ",");

            for (String branchPattern : branches) {

                String branchName = branchPattern.replaceAll("[^a-zA-Z0-9]", StringUtils.EMPTY);

                Optional<MetaData> optionalMetaData = workflows.stream().filter(it -> StringUtils.equalsIgnoreCase(it.getBranchName(), branchName)).findFirst();

                MetaData metaData = new MetaData();

                if (optionalMetaData.isPresent()) {
                    metaData = optionalMetaData.get();
                } else {
                    workflows.add(metaData);
                }

                List<String> stageNames = metaData.getStageNames();

                stageNames.add(stageName);

                metaData.setStageName(stageName);
                metaData.setBranchName(branchName);
                metaData.setBranchPattern(branchPattern);
            }
        }

        return workflows;
    }

    void executeImpl(MetaData metaData, List<MetaData> workflows) {

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

        defaultVariables.put("VERSION", version);

        String pipeline = PipelineGeneratorUtil.getTemplate("pipeline");

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", getPipelineName(metaData))
                .replace("%BRANCH_NAME%", metaData.getBranchPattern())
                .replace("  %ENV%", getVariablesTemplate(defaultVariables))
                .replace("  %JOBS%", getStagesTemplate(metaData));

        String workflowFileName = getWorkflowFileName(metaData, workflows);

        File githubWorkflow = new File(dir, workflowFileName);

        logMessage("Generate Github Workflows Pipeline for " + appName + " -> " + workflowFileName);

        if (PipelineGeneratorUtil.hasMavenWrapper(project)) {
            pipeline = pipeline.replaceAll("mvn ", "./mvnw ");
        }

        runsOn = Optional.ofNullable(runsOn).orElse("ubuntu-latest");

        runsOn = Stream.of(runsOn.split(",")).map(StringUtils::trim).collect(Collectors.joining(", "));

        boolean supportVersionJob = Stream.of("develop", "feature").noneMatch(it -> StringUtils.equalsIgnoreCase(it, metaData.getBranchName()));

        pipeline = pipeline
                .replaceAll("%RUNS_ON%", String.join(", ", runsOn))
                .replaceAll("%POM_ARTIFACT%", String.valueOf(supportVersionJob));

        try (PrintWriter out = new PrintWriter(githubWorkflow)) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getPipelineName(MetaData metaData) {

        if (StringUtils.equalsIgnoreCase(metaData.getStageName(), "none")) {

            return appName;
        }

        if (!PipelineGeneratorUtil.isMicroserviceRepo(project)) {

            return appName;
        }

        return ("[" + String.join(", ", metaData.getStageNames()) + "] ").toUpperCase() + appName;
    }


    public String getVariablesTemplate(Map<String, String> branchVariables) {

        branchVariables.entrySet().forEach(it -> it.setValue(unMask(it.getValue())));

        String template = null;
        try {
            template = objectMapper.writeValueAsString(Collections.singletonMap("env", branchVariables));
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

    public MavenProject getProject() {

        return project;
    }

    public Map<String, String> getDownStreams() {

        return downStreams;
    }

    public List<String> getRunsOn() {

        return Stream.of(runsOn.split(",")).map(StringUtils::trim).collect(Collectors.toList());
    }

    public Map<String, Object> getVariables() {

        return new LinkedHashMap<>(variables);
    }
}
