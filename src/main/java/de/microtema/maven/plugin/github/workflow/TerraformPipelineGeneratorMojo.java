package de.microtema.maven.plugin.github.workflow;

import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.job.terraform.*;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import de.microtema.model.converter.util.ClassUtil;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil.*;

public class TerraformPipelineGeneratorMojo extends PipelineGeneratorMojo {

    private final PipelineTemplateStageService pipelineTemplateStageService;

    public TerraformPipelineGeneratorMojo(PipelineGeneratorMojo mojo) {
        this.project = mojo.project;
        this.downStreams = mojo.downStreams;
        this.stages = mojo.stages;
        this.variables = mojo.variables;
        this.githubWorkflowsDir = mojo.githubWorkflowsDir;
        this.runsOn = mojo.runsOn;
        this.appName = mojo.getAppDisplayName();
        this.pipelineTemplateStageService = ClassUtil.createInstance(PipelineTemplateStageService.class);
    }

    public void execute() {

        injectTemplateStageServices();

        File rootDir = getOrCreateWorkflowsDir();

        cleanupWorkflows(rootDir);

        applyDefaultVariables();

        List<MetaData> workflows = getWorkflowFiles(project, stages, downStreams);

        for (MetaData metaData : workflows) {
            executeImpl(metaData, workflows);
        }
    }

    void injectTemplateStageServices() {

        templateStageServices.add(ClassUtil.createInstance(InitializeTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(ValidateTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PackagingTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DeploymentTemplateStageService.class));
    }

    void applyDefaultVariables() {

        defaultVariables.put("APP_NAME", project.getArtifactId());
        defaultVariables.put("APP_DISPLAY_NAME", appName);
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
                version = version.replace("-SNAPSHOT", "");
                break;
            case "master":
                version = version.replace("-SNAPSHOT", "");
                version = version.replace("-RC", "");
                version = version.replace("-FIX", "");
                break;
        }

        defaultVariables.put("VERSION", version);

        String pipeline = pipelineTemplateStageService.getTemplate(this, metaData);

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", getPipelineName(metaData))
                .replace("%VERSION%", version)
                .replace("%BRANCH_NAME%", metaData.getBranchPattern())
                .replace("  %ENV%", getVariablesTemplate(defaultVariables))
                .replace("  %JOBS%", getStagesTemplate(metaData, templateStageServices));

        String workflowFileName = getWorkflowFileName(metaData, workflows);

        File githubWorkflow = new File(dir, workflowFileName);

        logMessage("Generate Github Workflows Pipeline for " + appName + " -> " + workflowFileName);

        pipeline = pipeline.replaceAll("%RUNS_ON%", String.join(", ", runsOn));

        pipeline = PipelineGeneratorUtil.removeEmptyLines(pipeline);

        try (PrintWriter out = new PrintWriter(githubWorkflow)) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    String getStagesTemplate(MetaData metaData, List<TemplateStageService> templateStageServices) {

        return templateStageServices.stream()
                .map(it -> it.getTemplate(this, metaData))
                .filter(Objects::nonNull)
                .map(it -> PipelineGeneratorUtil.trimEmptyLines(it, 2))
                .collect(Collectors.joining("\n\n"));
    }
}
