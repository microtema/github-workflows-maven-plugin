package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class DependencyCheckTemplateStageService implements TemplateStageService {

    @Override
    public String getJobId() {
        return "security-check";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (PipelineGeneratorUtil.isSpeedBranch(metaData.getBranchName())) {
            return false;
        }

        return PipelineGeneratorUtil.hasSourceCode(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getTemplateName());
    }
}
