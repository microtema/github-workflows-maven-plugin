package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeploymentTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.existsDockerfile(project)) {
            return null;
        }

        String template = getStagesTemplate(mojo);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    @Override
    public String getStagesTemplate(PipelineGeneratorMojo mojo) {

        Map<String, String> stages = mojo.getStages();

        List<String> stageNames = stages.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());

        return stages.entrySet().stream().map(it -> {

            String name = it.getKey();
            List<String> branches = Arrays.asList(StringUtils.split(it.getValue(), ","));

            return getTemplate(name.toLowerCase(), branches, stageNames);
        }).collect(Collectors.joining("\n"));
    }

    String getTemplate(String env, List<String> branches, List<String> stageNames) {

        String glabVariables = stageNames.stream()
                .map(it -> String.format("ENABLE_ENV_%s:%s", it.toUpperCase(), StringUtils.equalsIgnoreCase(it, env)))
                .collect(Collectors.joining(","));
        glabVariables += ",DEPLOYMENT_STRATEGY:auto-master";

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%GLAB_VARIABLES%", glabVariables)
                .replace("%STAGE_DISPLAY_NAME%", "Deployment:" + env.toUpperCase())
                .replace("%REFS%", "[ " + String.join(", ", branches) + " ]");
    }
}
