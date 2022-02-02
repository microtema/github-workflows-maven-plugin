package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil.parseTestType;

public class SystemTestTemplateStageService implements TemplateStageService {

    private final ReadinessTemplateStageService readinessTemplateStageService;

    public SystemTestTemplateStageService(ReadinessTemplateStageService readinessTemplateStageService) {
        this.readinessTemplateStageService = readinessTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject())) {
            return false;
        }

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        List<String> regressionTestTypes = PipelineGeneratorUtil.getRegressionTestTypes(mojo.getProject());

        return !regressionTestTypes.isEmpty();
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = getStagesTemplate(mojo, metaData);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    private String getStagesTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        List<String> stageNames = metaData.getStageNames();

        List<String> regressionTestTypes = PipelineGeneratorUtil.getRegressionTestTypes(mojo.getProject());

        boolean multipleStages = stageNames.size() > 1;

        return stageNames.stream().map(it -> {

            String defaultTemplate = regressionTestTypes.stream()
                    .map(f -> getTemplate(f, it, regressionTestTypes.size() > 1, multipleStages))
                    .collect(Collectors.joining("\n"));

            String needs = readinessTemplateStageService.getJobNames(metaData, it);

            return defaultTemplate.replace("%NEEDS%", needs);

        }).collect(Collectors.joining("\n"));
    }

    private String getTemplate(String testType, String stageName, boolean multipleTests, boolean multipleStages) {

        String template = PipelineGeneratorUtil.getTemplate(getName());

        template = PipelineGeneratorUtil.applyProperties(template, stageName);

        String jobId = "system-test";
        String jobName = "System Test";

        if (multipleStages) {
            jobId += "-" + stageName.toLowerCase();
            jobName += " [" + stageName.toUpperCase() + "]";
        }

        if (multipleTests) {
            jobId += "-" + testType.toLowerCase();
            jobName += "(" + testType.toUpperCase() + ")";
        }

        return template
                .replace("system-test:", jobId + ":")
                .replace("%JOB_NAME%", jobName)
                .replace("%TEST_TYPE%", parseTestType(testType))
                .replace("%SOURCE_TYPE%", testType)
                .replace("%STAGE_NAME%", stageName.toLowerCase());
    }
}
