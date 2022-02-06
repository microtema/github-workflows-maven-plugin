package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.JobData;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.List;
import java.util.stream.Collectors;

public interface TemplateStageService {

    String regex = "([a-z0-9])([A-Z])";
    String replacement = "$1-$2";

    default String getTemplateName() {

        return getClass().getSimpleName()
                .replace("TemplateStageService", StringUtils.EMPTY)
                .replaceAll(regex, replacement).toLowerCase();
    }

    default String getJobId() {

        return getTemplateName();
    }

    default String getJobName() {

        return WordUtils.capitalize(getTemplateName());
    }

    default JobData getJobData() {

        JobData jobData = new JobData();

        jobData.setId(getJobId());
        jobData.setName(getJobName());

        return jobData;
    }

    default String getJobIds(MetaData metaData, String stageName) {

        List<String> stageNames = metaData.getStageNames();

        String jobName = getJobId();

        if (CollectionUtils.size(stageNames) == 1) {
            return jobName;
        }

        return stageNames.stream()
                .filter(it -> StringUtils.equalsIgnoreCase(it, stageName))
                .map(it -> jobName + "-" + it)
                .collect(Collectors.joining(" "));
    }

    default String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        return PipelineGeneratorUtil.getTemplate(getTemplateName());
    }

    default boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return false;
    }
}
