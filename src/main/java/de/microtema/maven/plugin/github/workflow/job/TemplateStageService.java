package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface TemplateStageService {

    default String getName() {

        return getClass().getSimpleName().replace("TemplateStageService", "").toLowerCase();
    }

    default String getJobName() {

        return getName();
    }

    default String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        return PipelineGeneratorUtil.getTemplate(getName());
    }

    default boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return false;
    }

    default String getTemplate(String template, List<String> stages) {
        return template;
    }

    default String getStagesTemplate(PipelineGeneratorMojo mojo) {

        Map<String, String> stages = mojo.getStages();

        return stages.entrySet().stream().map(it -> {

            String name = it.getKey();
            List<String> branches = Arrays.asList(StringUtils.split(it.getValue(), ","));

            return getTemplate(name, branches);
        }).collect(Collectors.joining("\n"));
    }
}
