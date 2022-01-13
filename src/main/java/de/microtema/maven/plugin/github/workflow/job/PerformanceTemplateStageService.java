package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerformanceTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsPerformanceTests(mojo.getProject())) {
            return null;
        }

        String template = getStagesTemplate(mojo);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    @Override
    public String getStagesTemplate(PipelineGeneratorMojo mojo) {

        Map<String, String> stages = mojo.getStages();

        return stages.entrySet().stream()
                .filter(it -> !StringUtils.equalsIgnoreCase(it.getKey(), "prod"))
                .map(it -> {

                    String name = it.getKey();
                    List<String> branches = Arrays.asList(StringUtils.split(it.getValue(), ","));

                    return getTemplate(name, branches);
                }).collect(Collectors.joining("\n"));
    }

    @Override
    public String getTemplate(String env, List<String> branches) {

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%STAGE_DISPLAY_NAME%", "Load and Performance:" + env.toUpperCase())
                .replace("%PROFILE_NAME%", "performance-" + env.toLowerCase())
                .replace("%STAGE_NAME%", env.toLowerCase())
                .replace("%REFS%", "[ " + String.join(", ", branches) + " ]");
    }
}
