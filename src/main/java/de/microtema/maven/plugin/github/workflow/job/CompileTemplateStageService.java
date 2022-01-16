package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class CompileTemplateStageService implements TemplateStageService {

    private final VersioningTemplateStageService versioningTemplateStageService;

    public CompileTemplateStageService(VersioningTemplateStageService versioningTemplateStageService) {
        this.versioningTemplateStageService = versioningTemplateStageService;
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        String template = PipelineGeneratorUtil.getTemplate(getName());

        return template.replace("%NEEDS%", versioningTemplateStageService.getName());
    }
}
