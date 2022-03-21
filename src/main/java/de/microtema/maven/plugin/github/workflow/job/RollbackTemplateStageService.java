package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class RollbackTemplateStageService implements TemplateStageService {

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {
        return true;
    }

    @Override
    public String getTemplateName() {
        return "helm-rollback";
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getTemplateName());
        String stageName = metaData.getStageName();
        template = PipelineGeneratorUtil.applyProperties(template, stageName);

        return template
                .replace("%JOB_NAME%", "[" + stageName.toUpperCase() + "] Rollback")
                .replace("%STAGE_NAME%", stageName);
    }
}
