package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class TagTemplateStageService implements TemplateStageService {

    private final BuildTemplateStageService buildTemplateStageService;
    private final PackageTemplateStageService packageTemplateStageService;

    public TagTemplateStageService(BuildTemplateStageService buildTemplateStageService,
                                   PackageTemplateStageService packageTemplateStageService) {
        this.buildTemplateStageService = buildTemplateStageService;
        this.packageTemplateStageService = packageTemplateStageService;
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

        String needs = getJobNeeds(mojo, metaData);

        return template.replace("%NEEDS%", needs).replace("%APP_DISPLAY_NAME%", mojo.getAppDisplayName());
    }

    private String getJobNeeds(PipelineGeneratorMojo mojo, MetaData metaData) {

        List<String> stageNames = metaData.getStageNames();
        boolean sameDockerRegistry = PipelineGeneratorUtil.isSameDockerRegistry(stageNames);

        if (packageTemplateStageService.access(mojo, metaData)) {

            if (sameDockerRegistry) {
                return packageTemplateStageService.getJobId();
            }

            return stageNames.stream()
                    .map(it -> packageTemplateStageService.getJobIds(metaData, it))
                    .collect(Collectors.joining(", "));
        }

        if (buildTemplateStageService.access(mojo, metaData)) {

            return buildTemplateStageService.getJobId();
        }

        return "compile";
    }
}
