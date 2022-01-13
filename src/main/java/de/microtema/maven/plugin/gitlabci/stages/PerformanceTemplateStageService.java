package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerformanceTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.existsPerformanceTests(project)) {
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
