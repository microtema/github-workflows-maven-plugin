package de.microtema.maven.plugin.github.workflow.job.terraform;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class UnDeploymentTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplateName() {
        return "terraform/undeploy";
    }

    @Override
    public String getJobId() {
        return "undeploy";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        if (!mojo.isUnDeploy()) {
            return false;
        }

        return PipelineGeneratorUtil.isDeploymentRepo(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

        String template = PipelineGeneratorUtil.applyProperties(defaultTemplate, metaData.getStageName());

        return template.replace("%WORKING_DIRECTORY%", "./terraform");
    }
}
