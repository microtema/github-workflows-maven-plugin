package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class PromoteTemplateStageService implements TemplateStageService {

    private final TagTemplateStageService tagTemplateStageService;

    private final HelmTemplateStageService helmTemplateStageService;

    public PromoteTemplateStageService(TagTemplateStageService tagTemplateStageService,
                                       HelmTemplateStageService helmTemplateStageService) {
        this.tagTemplateStageService = tagTemplateStageService;
        this.helmTemplateStageService = helmTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (helmTemplateStageService.access(mojo, metaData)) {
            return false;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
            return false;
        }

        return PipelineGeneratorUtil.existsDockerfile(mojo.getProject());
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
