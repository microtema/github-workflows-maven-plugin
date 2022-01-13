package de.microtema.maven.plugin.gitlabci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import de.microtema.maven.plugin.gitlabci.stages.*;
import de.microtema.model.converter.util.ClassUtil;
import lombok.SneakyThrows;
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

import static de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil.hasMavenWrapper;
import static de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil.isGitRepo;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class PipelineGeneratorMojo extends AbstractMojo {

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

    List<TemplateStageService> templateStageServices = new ArrayList<>();

    LinkedHashMap<String, String> defaultVariables = new LinkedHashMap<>();

    @SneakyThrows
    public void execute() {

        String appName = Optional.ofNullable(project.getName()).orElse(project.getArtifactId());

        // Skip maven sub modules
        if (!isGitRepo(project)) {

            logMessage("Skip maven module: " + appName + " since it is not a git repo!");

            return;
        }

        defaultVariables.put("GIT_STRATEGY", "clone");
        defaultVariables.put("GIT_DEPTH", "10");
        defaultVariables.put("MAVEN_CLI_OPTS", "-s settings.xml --batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true");

        defaultVariables.forEach((key, value) -> variables.putIfAbsent(key, value));

        templateStageServices.add(ClassUtil.createInstance(VersioningTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(CompileTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(SecurityTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(TestTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(SonarTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(BuildTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PackageTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DbMigrationTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(TagTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PublishTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PromoteTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DeploymentTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(ReadynessTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(RegressionTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PerformanceTemplateStageService.class));

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getValue();
            String[] branches = StringUtils.split(stage.getValue(), ",");

            for (String branchPattern : branches) {

                String branchName = branchPattern.replaceAll("[^a-zA-Z0-9]", StringUtils.EMPTY);

                executeImpl(appName, branchName, stageName);
            }
        }
    }

    void executeImpl(String appName, String branchName, String stageName) {

        String rootPath = PipelineGeneratorUtil.getRootPath(project);

        File dir = new File(rootPath, githubWorkflowsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String pipeline = PipelineGeneratorUtil.getTemplate("pipeline");

        pipeline = pipeline.replace("%PIPELINE_NAME%", appName);

        File githubWorkflow = new File(dir, branchName + ".yaml");

        logMessage("Generate Github Workflows Pipeline for " + appName + " -> " + githubWorkflow.getPath());

        if (hasMavenWrapper(project)) {
            pipeline = pipeline.replaceAll("mvn ", "./mvnw ");
        }

        try (PrintWriter out = new PrintWriter(githubWorkflow)) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
}
