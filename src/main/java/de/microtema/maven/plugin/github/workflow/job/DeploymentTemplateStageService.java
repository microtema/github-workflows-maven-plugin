package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeploymentTemplateStageService implements TemplateStageService {

    private final PromoteTemplateStageService promoteTemplateStageService;

    public DeploymentTemplateStageService(PromoteTemplateStageService promoteTemplateStageService) {
        this.promoteTemplateStageService = promoteTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!promoteTemplateStageService.access(mojo, metaData)) {
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

            String needs = promoteTemplateStageService.getJobIds(metaData, it);

            String workflow = mojo.getProject().getName() + (" [" + it + "]").toUpperCase();

            return defaultTemplate
                    .replace("deployment:", multipleStages ? "deployment-" + it.toLowerCase() + ":" : "deployment:")
                    .replace("%JOB_NAME%", multipleStages ? "Deployment [" + it.toUpperCase() + "]" : "Deployment")
                    .replace("%NEEDS%", needs)
                    .replace("%WORKFLOW%", workflow);

        }).collect(Collectors.joining("\n"));
    }
}
