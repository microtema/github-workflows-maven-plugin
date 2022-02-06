package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class DeploymentTemplateStageService implements TemplateStageService {

    private final HelmTemplateStageService helmTemplateStageService;

    public DeploymentTemplateStageService(HelmTemplateStageService helmTemplateStageService) {
        this.helmTemplateStageService = helmTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (helmTemplateStageService.access(mojo, metaData)) {
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

        return PipelineGeneratorUtil.getTemplate(getTemplateName()).replace("%BRANCH_NAME%", metaData.getBranchName());
    }
}
