package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class PublishTemplateStageService implements TemplateStageService {

    private final TagTemplateStageService tagTemplateStageService;

    public PublishTemplateStageService(TagTemplateStageService tagTemplateStageService) {
        this.tagTemplateStageService = tagTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return !PipelineGeneratorUtil.existsDockerfile(mojo.getProject());
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
