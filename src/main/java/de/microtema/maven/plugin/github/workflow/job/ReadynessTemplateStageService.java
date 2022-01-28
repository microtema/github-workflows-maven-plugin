package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadynessTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public ReadynessTemplateStageService(HelmTemplateStageService helmTemplateStageService,
                                         DeploymentTemplateStageService deploymentTemplateStageService) {
        this.templateStageServices.add(helmTemplateStageService);
        this.templateStageServices.add(deploymentTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!mojo.hasVariable("SERVICE_URL", metaData.getStageName())) {
            return false;
        }

        if (!PipelineGeneratorUtil.existsDockerfile(mojo.getProject())) {
            return false;
        }

        return Stream.of("feature", "bugfix").noneMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it));
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        String needs = templateStageServices.stream().filter(it -> it.access(mojo, metaData))
                .map(TemplateStageService::getJobName)
                .collect(Collectors.joining(", "));

        return template.replace("%NEEDS%", needs);
    }
}
