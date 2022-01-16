package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class PromoteTemplateStageService implements TemplateStageService {

    private final TagTemplateStageService tagTemplateStageService;

    public PromoteTemplateStageService(TagTemplateStageService tagTemplateStageService) {
        this.tagTemplateStageService = tagTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return PipelineGeneratorUtil.existsDockerfile(mojo.getProject()) && !StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature");
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        String needs = "package";

        if (tagTemplateStageService.access(mojo, metaData)) {
            needs = tagTemplateStageService.getJobName();
        }

        return template.replace("%NEEDS%", needs);
    }

}
