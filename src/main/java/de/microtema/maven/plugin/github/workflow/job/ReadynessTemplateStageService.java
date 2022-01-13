package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadynessTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsDockerfile(mojo.getProject())) {
            return null;
        }

        String template = getStagesTemplate(mojo);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    @Override
    public String getStagesTemplate(PipelineGeneratorMojo mojo) {

        Map<String, String> stages = mojo.getStages();

        return stages.entrySet().stream().map(it -> {

            String name = it.getKey();

            List<String> branches = Arrays.asList(StringUtils.split(it.getValue(), ","));

            String serviceUrl = mojo.getServiceUrl();

            return getTemplate(name, branches, serviceUrl, mojo.getClusters());
        }).collect(Collectors.joining("\n"));
    }

    private String getTemplate(String env, List<String> branches, String serviceUrl, Map<String, String> clusters) {

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%STAGE_DISPLAY_NAME%", "Readiness Check:" + env.toUpperCase())
                .replace("%SERVICE_URL%", getServiceUrl(serviceUrl, env, clusters))
                .replace("%REFS%", "[ " + String.join(", ", branches) + " ]");
    }

    private String getServiceUrl(String serviceUrl, String env, Map<String, String> clusters) {

        String cluster = clusters.get(env);

        return serviceUrl
                .replace("$STAGE", env.toLowerCase())
                .replace("$CLUSTER", cluster.toLowerCase());
    }
}
