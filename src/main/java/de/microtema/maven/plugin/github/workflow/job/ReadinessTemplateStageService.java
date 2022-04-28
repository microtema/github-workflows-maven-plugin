package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReadinessTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public ReadinessTemplateStageService(HelmTemplateStageService helmTemplateStageService,
                                         DeploymentTemplateStageService deploymentTemplateStageService) {
        this.templateStageServices.add(helmTemplateStageService);
        this.templateStageServices.add(deploymentTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!metaData.isDeployable()) {
            return false;
        }

        return metaData.getStageNames()
                .stream()
                .map(PipelineGeneratorUtil::findProperties)
                .filter(Objects::nonNull)
                .anyMatch(it -> it.containsKey("SERVICE_URL"));
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

            boolean privateNetwork = PipelineGeneratorUtil.isPrivateNetwork(it);

            String needs = templateStageServices.stream().filter(e -> e.access(mojo, metaData))
                    .map(e -> e.getJobIds(metaData, it))
                    .collect(Collectors.joining(", "));

            return defaultTemplate
                    .replace("readiness:", multipleStages ? "readiness-" + it.toLowerCase() + ":" : "readiness:")
                    .replace("%JOB_NAME%", PipelineGeneratorUtil.getJobName("Readiness Check", it, multipleStages))
                    .replaceAll("%PRIVATE_NETWORK%", String.valueOf(privateNetwork))
                    .replace("%NEEDS%", needs);

        }).collect(Collectors.joining("\n"));
    }
}
