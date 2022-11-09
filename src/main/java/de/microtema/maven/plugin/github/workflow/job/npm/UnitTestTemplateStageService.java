package de.microtema.maven.plugin.github.workflow.job.npm;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class UnitTestTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplateName() {
        return "npm/unit-test";
    }

    @Override
    public String getJobId() {

        return "unit-test";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return PipelineGeneratorUtil.isNodeJsRepo(mojo.getProject());
    }
}
