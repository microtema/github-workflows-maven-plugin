package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public interface TemplateStageService {

    String regex = "([a-z0-9])([A-Z])";
    String replacement = "$1-$2";

    default String getName() {

        return getClass().getSimpleName()
                .replace("TemplateStageService", StringUtils.EMPTY)
                .replaceAll(regex, replacement).toLowerCase();
    }

    default String getJobName() {

        return getName();
    }

    default String getJobNames(MetaData metaData, String stageName) {

        List<String> stageNames = metaData.getStageNames();

        String jobName = getJobName();

        if (CollectionUtils.size(stageNames) == 1) {
            return jobName;
        }

        return stageNames.stream()
                .filter(it -> StringUtils.equalsIgnoreCase(it, stageName))
                .map(it -> jobName + "-" + it)
                .collect(Collectors.joining(" "));
    }

    default String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        return PipelineGeneratorUtil.getTemplate(getName());
    }

    default boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return false;
    }
}
