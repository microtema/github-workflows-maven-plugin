package de.microtema.maven.plugin.github.workflow;

import de.microtema.maven.plugin.github.workflow.job.*;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import de.microtema.model.converter.util.ClassUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil.*;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class PipelineGeneratorMojo extends AbstractMojo {

    String workflowFilePostFixName = "-workflow.yaml";

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

    @Parameter(property = "generate-rollback")
    boolean generateRollback;

    @Parameter(property = "undeploy")
    boolean undeploy;

    final List<TemplateStageService> templateStageServices = new ArrayList<>();
    final LinkedHashMap<String, String> defaultVariables = new LinkedHashMap<>();

    String appName;

    public void execute() {

        appName = getAppDisplayName();

        runsOn = Optional.ofNullable(runsOn).orElse("ubuntu-latest");
        runsOn = Stream.of(runsOn.split(",")).map(StringUtils::trim).collect(Collectors.joining(", "));

        // Skip maven sub modules
        if (!PipelineGeneratorUtil.isGitRepo(project)) {

            logMessage("Skip maven module: " + appName + " since it is not a git repo!");

            return;
        }

        if (PipelineGeneratorUtil.isNodeJsRepo(project)) {

            NpmPipelineGeneratorMojo npmPipelineGeneratorMojo = new NpmPipelineGeneratorMojo(this);

            npmPipelineGeneratorMojo.execute();

            return;
        }

        if (PipelineGeneratorUtil.isTerraformRepo(project)) {

            TerraformPipelineGeneratorMojo terraformPipelineGeneratorMojo = new TerraformPipelineGeneratorMojo(this);

            terraformPipelineGeneratorMojo.execute();

            return;
        }

        injectTemplateStageServices();

        File rootDir = getOrCreateWorkflowsDir();

        cleanupWorkflows(rootDir);

        applyDefaultVariables();

        List<MetaData> workflows = getWorkflowFiles(project, stages, downStreams);

        for (MetaData metaData : workflows) {
            executeImpl(metaData, workflows);
        }

        if (!generateRollback) {
            return;
        }

        // Generate rollback workflows
        workflows = getRollbackWorkflowFiles();

        for (MetaData metaData : workflows) {
            executeRollbackImpl(metaData);
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
        templateStageServices.add(ClassUtil.createInstance(UnDeploymentTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PerformanceTestTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DownstreamTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(NotificationTemplateStageService.class));
    }

    void applyDefaultVariables() {

        defaultVariables.put("APP_NAME", project.getArtifactId());
        defaultVariables.put("APP_DISPLAY_NAME", appName);

        if (PipelineGeneratorUtil.isDeploymentRepo(project)) {
            return;
        }

        defaultVariables.put("GITHUB_TOKEN", "${{ secrets.GITHUB_TOKEN }}");

        if (PipelineGeneratorUtil.hasSonarProperties(project)) {

            String sonarToken = PipelineGeneratorUtil.getProperty(project, "sonar.login", "${{ secrets.SONAR_TOKEN }}");

            sonarToken = variables.getOrDefault("SONAR_TOKEN", sonarToken);

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
            for (Map.Entry<String, String> entry : variables.entrySet()) {

                String key = entry.getKey();

                if (StringUtils.startsWith(key, "ARTIFACTORY")) {
                    String orDefault = variables.getOrDefault(key, "${{ secrets." + key + " }}");
                    defaultVariables.put(key, wrapSecretVariable(orDefault));
                }
            }
        }

        defaultVariables.put("MAVEN_CLI_OPTS", mavenCliOptions);

        String codePaths = ".github/** src/** pom.xml";

        if (PipelineGeneratorUtil.existsDockerfile(project)) {
            codePaths += " Dockerfile";
        }

        if (PipelineGeneratorUtil.isMicroserviceRepo(project)) {

            defaultVariables.putIfAbsent("CODE_PATHS", codePaths);
        }

        if (PipelineGeneratorUtil.isMavenArtifactRepo(project)) {

            defaultVariables.putIfAbsent("CODE_PATHS", "*");
        }
    }

    private String wrapSecretVariable(String variableValue) {

        if (variableValue.startsWith("secrets.")) {

            return "${{ " + variableValue + " }}";
        }

        return variableValue;
    }

    File getOrCreateWorkflowsDir() {

        String rootPath = getRootPath(project);

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

        String branchName = metaData.getBranchFullName();

        boolean duplicate = workflows.stream().filter(it -> StringUtils.equalsIgnoreCase(it.getBranchFullName(), branchName)).count() > 1;

        String workflowName = branchName + workflowFilePostFixName;

        if (duplicate) {
            workflowName = branchName + "-" + metaData.getStageName() + workflowFilePostFixName;
        }

        return workflowName;
    }

    List<MetaData> getRollbackWorkflowFiles() {

        if (!PipelineGeneratorUtil.isMicroserviceRepo(project)) {

            return Collections.emptyList();
        }

        List<MetaData> workflows = new ArrayList<>();

        for (String stageName : stages.keySet()) {

            if (StringUtils.equalsIgnoreCase(stageName, "none")) {
                continue;
            }

            MetaData metaData = new MetaData();

            metaData.setStageName(stageName);

            workflows.add(metaData);
        }

        return workflows;
    }

    void executeImpl(MetaData metaData, List<MetaData> workflows) {

        String rootPath = getRootPath(project);

        File dir = new File(rootPath, githubWorkflowsDir);

        String version = getVersion(metaData.getBranchName(), project.getVersion());

        defaultVariables.put("VERSION", version);

        String pipeline = PipelineGeneratorUtil.getTemplate("pipeline");

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", getPipelineName(project, metaData, appName))
                .replace("%VERSION%", version)
                .replace("%BRANCH_NAME%", metaData.getBranchPattern())
                .replace("  %ENV%", getVariablesTemplate(defaultVariables))
                .replace("  %JOBS%", getStagesTemplate(metaData, templateStageServices));

        String workflowFileName = getWorkflowFileName(metaData, workflows);

        File githubWorkflow = new File(dir, workflowFileName);

        logMessage("Generate Github Workflows Pipeline for " + appName + " -> " + workflowFileName);

        if (PipelineGeneratorUtil.hasMavenWrapper(project)) {
            pipeline = pipeline.replaceAll("mvn ", "./mvnw ");
        }

        boolean supportVersionJob = Stream.of("develop", "feature").noneMatch(it -> StringUtils.equalsIgnoreCase(it, metaData.getBranchName()));

        pipeline = pipeline
                .replaceAll("%RUNS_ON%", String.join(", ", runsOn))
                .replaceAll("%POM_ARTIFACT%", "'" + supportVersionJob + "'");

        pipeline = PipelineGeneratorUtil.removeEmptyLines(pipeline);

        try (PrintWriter out = new PrintWriter(githubWorkflow)) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void executeRollbackImpl(MetaData metaData) {

        String rootPath = getRootPath(project);

        File dir = new File(rootPath, githubWorkflowsDir);

        String pipeline = PipelineGeneratorUtil.getTemplate("pipeline-rollback");

        RollbackTemplateStageService rollbackTemplateStageService = ClassUtil.createInstance(RollbackTemplateStageService.class);

        Map<String, String> templateVariables = new HashMap<>(Collections.singletonMap("APP_NAME", project.getArtifactId()));

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", getRollbackPipelineName(metaData.getStageName(), appName))
                .replace("  %ENV%", getVariablesTemplate(templateVariables))
                .replace("  %JOBS%", getStagesTemplate(metaData, Collections.singletonList(rollbackTemplateStageService)));

        String workflowFileName = metaData.getStageName() + "-rollback" + workflowFilePostFixName;

        File githubWorkflow = new File(dir, workflowFileName);

        logMessage("Generate Github Workflows Pipeline for (rollback) " + appName + " -> " + workflowFileName);

        pipeline = pipeline.replaceAll("%RUNS_ON%", String.join(", ", runsOn));

        pipeline = PipelineGeneratorUtil.removeEmptyLines(pipeline);

        try (PrintWriter out = new PrintWriter(githubWorkflow)) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void executeUndeployImpl(MetaData metaData) {

    }

    String getStagesTemplate(MetaData metaData, List<TemplateStageService> templateStageServices) {

        return templateStageServices.stream()
                .map(it -> it.getTemplate(this, metaData))
                .filter(Objects::nonNull)
                .map(it -> PipelineGeneratorUtil.trimEmptyLines(it, 2))
                .collect(Collectors.joining("\n\n"));
    }

    public MavenProject getProject() {

        return project;
    }

    public Map<String, String> getDownStreams() {

        return new LinkedHashMap<>(downStreams);
    }

    public Map<String, Object> getVariables() {

        return new LinkedHashMap<>(variables);
    }

    public String getAppDisplayName() {

        return Optional.ofNullable(project.getName()).orElse(project.getArtifactId());
    }

    public boolean isUnDeploy() {

        return undeploy;
    }
}
