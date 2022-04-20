package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class PublishTemplateStageService implements TemplateStageService {

    private final BuildTemplateStageService buildTemplateStageService;

    private final TagTemplateStageService tagTemplateStageService;

    public PublishTemplateStageService(BuildTemplateStageService buildTemplateStageService,
                                       TagTemplateStageService tagTemplateStageService) {
        this.buildTemplateStageService = buildTemplateStageService;
        this.tagTemplateStageService = tagTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return PipelineGeneratorUtil.isMavenArtifactRepo(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getTemplateName());
        template = PipelineGeneratorUtil.applyProperties(template, mojo.getVariables());

        String needs = buildTemplateStageService.getJobId();

        if (tagTemplateStageService.access(mojo, metaData)) {

            needs = tagTemplateStageService.getJobId();
        }

        return template.replace("%NEEDS%", needs);
    }
}
