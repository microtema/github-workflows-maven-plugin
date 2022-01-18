package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PerformanceTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public PerformanceTemplateStageService(RegressionTemplateStageService regressionTemplateStageService,
                                           ReadynessTemplateStageService readynessTemplateStageService) {
        this.templateStageServices.add(regressionTemplateStageService);
        this.templateStageServices.add(readynessTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "master")) {
            return false;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
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
