package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TagTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public TagTemplateStageService(BuildTemplateStageService buildTemplateStageService,
                                   PackageTemplateStageService packageTemplateStageService) {
        templateStageServices.add(packageTemplateStageService);
        templateStageServices.add(buildTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return StringUtils.equalsIgnoreCase(metaData.getBranchName(), "master");
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getTemplateName());

        String needs = templateStageServices.stream()
                .filter(it -> it.access(mojo, metaData))
                .findFirst()
                .map(TemplateStageService::getJobId)
                .orElse("compile");

        return template.replace("%NEEDS%", needs).replace("%APP_DISPLAY_NAME%", mojo.getAppDisplayName());
    }
}
