package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class HelmTemplateStageService implements TemplateStageService {

    private final VersioningTemplateStageService versioningTemplateStageService;
    private final PackageTemplateStageService packageTemplateStageService;

    public HelmTemplateStageService(VersioningTemplateStageService versioningTemplateStageService,
                                    PackageTemplateStageService packageTemplateStageService) {
        this.versioningTemplateStageService = versioningTemplateStageService;
        this.packageTemplateStageService = packageTemplateStageService;
    }

    @Override
    public String getJobId() {
        return "deployment";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
            return false;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "bugfix")) {
            return false;
        }

        return PipelineGeneratorUtil.existsHelmFile(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean multipleStages = stageNames.size() > 1;

        String template = stageNames.stream().map(it -> {

            String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it);

            return defaultTemplate
                    .replace("deployment:", multipleStages ? "deployment-" + it.toLowerCase() + ":" : "deployment:")
                    .replace("%JOB_NAME%", "[" + it.toUpperCase() + "] Deployment")
                    .replace("%STAGE_NAME%", it);

        }).collect(Collectors.joining("\n"));

        if (versioningTemplateStageService.access(mojo, metaData)) {

            template = template.replaceAll("%NEEDS%", packageTemplateStageService.getJobId());
        } else {

            template = template.replaceAll("\\[ %NEEDS% \\]", "[ ]");
        }

        return template;
    }
}
