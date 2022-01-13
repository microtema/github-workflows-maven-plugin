package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RegressionTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsDockerfile(mojo.getProject())) {
            return null;
        }

        String template = getStagesTemplate(mojo, mojo.getProject());

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    private String getStagesTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        List<String> regressionTestTypes = PipelineGeneratorUtil.getRegressionTestTypes(project);

        regressionTestTypes.removeIf(it -> !PipelineGeneratorUtil.existsRegressionTests(project, it));

        if (regressionTestTypes.isEmpty()) {
            return null;
        }

        Map<String, String> stages = mojo.getStages();

        return stages.entrySet().stream()
                .filter(it -> !StringUtils.equalsIgnoreCase(it.getKey(), "prod"))
                .map(it -> {
                    String name = it.getKey();
                    List<String> branches = Arrays.asList(StringUtils.split(it.getValue(), ","));

                    return regressionTestTypes.stream().map(f -> getTemplate(f, name, branches)).collect(Collectors.toList());
                }).flatMap(Collection::stream).collect(Collectors.joining("\n"));
    }

    private String getTemplate(String testType, String env, List<String> branches) {

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%STAGE_DISPLAY_NAME%", testType.toUpperCase() + ":" + env.toUpperCase())
                .replace("%PROFILE_NAME%", testType.toLowerCase())
                .replace("%STAGE_NAME%", env.toLowerCase())
                .replace("%REFS%", "[ " + String.join(", ", branches) + " ]");
    }
}
