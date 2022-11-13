package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil.getUndeployPipelineName;

public class UnDeploymentTemplateStageService implements TemplateStageService {

    private final ReadinessTemplateStageService readinessTemplateStageService;

    public UnDeploymentTemplateStageService(ReadinessTemplateStageService deploymentTemplateStageService) {
        this.readinessTemplateStageService = deploymentTemplateStageService;
    }

    @Override
    public String getTemplateName() {
        return "undeploy";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!readinessTemplateStageService.access(mojo, metaData)) {
            return false;
        }

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean multipleStages = stageNames.size() > 1;

        return stageNames.stream().map(it -> {

            String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it, mojo.getVariables());

            String needs = readinessTemplateStageService.getJobIds(metaData, it);

            return defaultTemplate
                    .replace("undeploy:", multipleStages ? "undeploy-" + it.toLowerCase() + ":" : "undeploy:")
                    .replace("%JOB_NAME%", PipelineGeneratorUtil.getJobName("Undeploy", it.toUpperCase(), multipleStages))
                    .replace("%NEEDS%", needs)
                    .replace("%WORKFLOW%", getUndeployPipelineName(it, mojo.getProject().getName()));

        }).collect(Collectors.joining("\n"));
    }
}
