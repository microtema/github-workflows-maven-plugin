package de.microtema.maven.plugin.github.workflow;

import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.job.terraform.*;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import de.microtema.model.converter.util.ClassUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
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
        this.undeploy = mojo.undeploy;
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

        if (!undeploy) {
            return;
        }

        // Generate undeploy workflows
        workflows = getUndeployWorkflowFiles();

        for (MetaData metaData : workflows) {
            executeUndeployImpl(metaData);
        }
    }

    void injectTemplateStageServices() {

        templateStageServices.add(ClassUtil.createInstance(InitializeTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(ValidateTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(PackageTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(DeploymentTemplateStageService.class));
    }

    void applyDefaultVariables() {

        defaultVariables.put("APP_NAME", project.getArtifactId());
        defaultVariables.put("APP_DISPLAY_NAME", appName);
    }

    void executeImpl(MetaData metaData, List<MetaData> workflows) {

        String rootPath = PipelineGeneratorUtil.getRootPath(project);

        File dir = new File(rootPath, githubWorkflowsDir);

        String version = getVersion(metaData.getBranchName(), project.getVersion());

        defaultVariables.put("VERSION", version);

        String pipeline = pipelineTemplateStageService.getTemplate(this, metaData);

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", getPipelineName(project, metaData, appName))
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

    List<MetaData> getUndeployWorkflowFiles() {

        List<MetaData> workflows = new ArrayList<>();

        for (String stageName : stages.keySet()) {

            if (StringUtils.equalsIgnoreCase(stageName, "none")) {
                continue;
            }

            String branchPattern = stages.get(stageName);

            MetaData metaData = new MetaData();

            metaData.setStageName(stageName);
            metaData.setBranchName(branchPattern.replaceAll("[^a-zA-Z0-9]", StringUtils.EMPTY));
            metaData.setBranchPattern(branchPattern);

            workflows.add(metaData);
        }

        return workflows;
    }

    void executeUndeployImpl(MetaData metaData) {

        String rootPath = getRootPath(project);

        File dir = new File(rootPath, githubWorkflowsDir);

        String pipeline = PipelineGeneratorUtil.getTemplate("terraform/undeploy-pipeline");

        List<TemplateStageService> templateStageServices = new ArrayList<>();

        templateStageServices.add(ClassUtil.createInstance(InitializeTemplateStageService.class));
        templateStageServices.add(ClassUtil.createInstance(UnDeploymentTemplateStageService.class));

        Map<String, String> templateVariables = new HashMap<>(Collections.singletonMap("APP_NAME", project.getArtifactId()));

        String version = getVersion(metaData.getBranchName(), project.getVersion());

        defaultVariables.put("VERSION", version);

        pipeline = pipeline
                .replace("%PIPELINE_NAME%", getUndeployPipelineName(metaData.getStageName(), appName))
                .replace("%BRANCH_NAME%", metaData.getBranchPattern())
                .replace("%VERSION%", version)
                .replace("  %ENV%", getVariablesTemplate(templateVariables))
                .replace("  %JOBS%", getStagesTemplate(metaData, templateStageServices));

        String workflowFileName = metaData.getBranchName() + "-undeploy" + workflowFilePostFixName;

        File githubWorkflow = new File(dir, workflowFileName);

        logMessage("Generate Github Workflows Pipeline for (undeploy) " + appName + " -> " + workflowFileName);

        pipeline = pipeline.replaceAll("%RUNS_ON%", runsOn);

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
