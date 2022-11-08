package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.List;
import java.util.stream.Collectors;

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

        if (!metaData.isDeployable()) {
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
                    .replace("%JOB_NAME%", PipelineGeneratorUtil.getJobName("Promote Env", it, multipleStages))
                    .replace("%NEEDS%", needs);

        }).collect(Collectors.joining("\n"));
    }

}
