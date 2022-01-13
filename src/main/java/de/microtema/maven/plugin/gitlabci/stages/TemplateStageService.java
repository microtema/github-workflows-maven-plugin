package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface TemplateStageService {

    default String getName() {

        return getClass().getSimpleName().replace("TemplateStageService", "").toLowerCase();
    }

    default String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        return PipelineGeneratorUtil.getTemplate(getName());
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
