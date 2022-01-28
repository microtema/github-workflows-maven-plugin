package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class PublishTemplateStageService implements TemplateStageService {

    private final TagTemplateStageService tagTemplateStageService;

    private final HelmTemplateStageService helmTemplateStageService;

    public PublishTemplateStageService(TagTemplateStageService tagTemplateStageService,
                                       HelmTemplateStageService helmTemplateStageService) {
        this.tagTemplateStageService = tagTemplateStageService;
        this.helmTemplateStageService = helmTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return !PipelineGeneratorUtil.existsDockerfile(mojo.getProject()) && !helmTemplateStageService.access(mojo, metaData);
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        String needs = "build";

        if (tagTemplateStageService.access(mojo, metaData)) {

            needs = tagTemplateStageService.getJobName();
        }

        return template.replace("%NEEDS%", needs);
    }
}
