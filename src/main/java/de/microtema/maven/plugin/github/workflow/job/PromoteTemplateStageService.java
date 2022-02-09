package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PromoteTemplateStageService implements TemplateStageService {

    private final TagTemplateStageService tagTemplateStageService;

    private final HelmTemplateStageService helmTemplateStageService;

    private final PackageTemplateStageService packageTemplateStageService;

    public PromoteTemplateStageService(TagTemplateStageService tagTemplateStageService,
                                       HelmTemplateStageService helmTemplateStageService,
                                       PackageTemplateStageService packageTemplateStageService) {
        this.tagTemplateStageService = tagTemplateStageService;
        this.helmTemplateStageService = helmTemplateStageService;
        this.packageTemplateStageService = packageTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (helmTemplateStageService.access(mojo, metaData)) {
            return false;
        }

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean multipleStages = stageNames.size() > 1;

        return stageNames.stream().map(it -> {

            String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it);

            String needs = packageTemplateStageService.getJobId();

            if (tagTemplateStageService.access(mojo, metaData)) {
                needs = tagTemplateStageService.getJobId();
            }

            return defaultTemplate
                    .replace("promote:", multipleStages ? "promote-" + it.toLowerCase() + ":" : "promote:")
                    .replace("%JOB_NAME%", "[" + it.toUpperCase() + "] Promote")
                    .replace("%NEEDS%", needs);

        }).collect(Collectors.joining("\n"));
    }

}
