package de.microtema.maven.plugin.github.workflow.job.npm;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

public class IntegrationTestTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplateName() {

        return "npm/integration-test";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return PipelineGeneratorUtil.hasE2ETests(mojo.getProject());
    }

    @Override
    public String getJobId() {
        return "it-test";
    }
}
