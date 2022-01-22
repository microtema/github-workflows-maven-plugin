package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class HelmTemplateStageService implements TemplateStageService {

    private final VersioningTemplateStageService versioningTemplateStageService;
    private final PackageTemplateStageService packageTemplateStageService;

    public HelmTemplateStageService(VersioningTemplateStageService versioningTemplateStageService,
                                    PackageTemplateStageService packageTemplateStageService) {
        this.versioningTemplateStageService = versioningTemplateStageService;
        this.packageTemplateStageService = packageTemplateStageService;
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

        String template = PipelineGeneratorUtil.getTemplate(getName());

        if (versioningTemplateStageService.access(mojo, metaData)) {

            template = template.replaceAll("%NEEDS%", packageTemplateStageService.getJobName());
        } else {

            template = template.replaceAll("\\[ %NEEDS% \\]", "[ ]");
        }

        return template.replaceAll("%STAGE_NAME%", metaData.getStageName().toLowerCase());
    }
}
