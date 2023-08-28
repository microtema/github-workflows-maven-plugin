package de.microtema.maven.plugin.github.workflow.job.terraform;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class InitializeTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplateName() {
        return "terraform/initialize";
    }

    @Override
    public String getJobId() {
        return "initialize";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
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

        return template
                .replace("%RUNS_ON%", "self-hosted")
                .replace("%APP_NAME%", metaData.getApplicationName().toLowerCase())
                .replaceAll("%STAGE_NAME%", metaData.getBranchName().toLowerCase())
                .replace("%TERRAFORM_VERSION%", "v1.5.5")
                .replace("%WORKING_DIRECTORY%", "./terraform");
    }
}
