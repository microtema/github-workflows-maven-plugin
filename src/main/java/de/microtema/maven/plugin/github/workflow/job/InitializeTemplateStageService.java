package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class InitializeTemplateStageService implements TemplateStageService {

    private final TerraformInitTemplateStageService terraformInitTemplateStageService;

    public InitializeTemplateStageService(TerraformInitTemplateStageService terraformInitTemplateStageService) {
        this.terraformInitTemplateStageService = terraformInitTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return true;
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (terraformInitTemplateStageService.access(mojo, metaData)) {
            return terraformInitTemplateStageService.getTemplate(mojo, metaData);
        }

        if (!access(mojo, metaData)) {
            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getTemplateName());
    }
}
