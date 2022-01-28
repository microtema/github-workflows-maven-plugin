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
                                               ReadynessTemplateStageService readynessTemplateStageService) {
        this.templateStageServices.add(regressionTemplateStageService);
        this.templateStageServices.add(readynessTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix", "master").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return PipelineGeneratorUtil.existsPerformanceTests(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        String stageName = metaData.getStageName();

        String needs = templateStageServices.stream().filter(it -> it.access(mojo, metaData))
                .map(TemplateStageService::getJobName)
                .collect(Collectors.joining(", "));

        return template
                .replace("%NEEDS%", needs)
                .replace("%STAGE_NAME%", stageName.toLowerCase());
    }
}
