package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformanceTestTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public PerformanceTestTemplateStageService(SystemTestTemplateStageService regressionTemplateStageService,
                                               ReadinessTemplateStageService readinessTemplateStageService) {
        this.templateStageServices.add(regressionTemplateStageService);
        this.templateStageServices.add(readinessTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (PipelineGeneratorUtil.isSpeedBranch(metaData.getBranchName())) {
            return false;
        }

        if (Stream.of("feature", "bugfix", "master").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        if (!PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject())) {
            return false;
        }

        return PipelineGeneratorUtil.existsPerformanceTests(mojo.getProject());
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

            String needs = templateStageServices.stream().filter(e -> e.access(mojo, metaData))
                    .map(e -> e.getJobIds(metaData, it))
                    .collect(Collectors.joining(", "));

            boolean privateNetwork = PipelineGeneratorUtil.isPrivateNetwork(it);

            return defaultTemplate
                    .replace("performance-test:", multipleStages ? "performance-test-" + it.toLowerCase() + ":" : "performance-test:")
                    .replace("%JOB_NAME%", "[" + it.toUpperCase() + "] Performance Test")
                    .replaceAll("%PRIVATE_NETWORK%", String.valueOf(privateNetwork))
                    .replace("%NEEDS%", needs);

        }).collect(Collectors.joining(System.lineSeparator()));
    }
}
