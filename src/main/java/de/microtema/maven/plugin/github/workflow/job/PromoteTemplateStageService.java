package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class PromoteTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsDockerfile(mojo.getProject())) {
            return null;
        }

        String template = getStagesTemplate(mojo);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }


    @Override
    public String getTemplate(String env, List<String> branches) {

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%STAGE_NAME%", env.toLowerCase())
                .replace("%RC_OR_FINAL%", getReleaseCandidateOrFinal(env.toLowerCase()))
                .replace("%CONTAINER_TAG%", getContainerTag(env.toLowerCase()))
                .replace("%STAGE_DISPLAY_NAME%", "Promote:" + env.toUpperCase())
                .replace("%REFS%", "[ " + String.join(", ", branches) + " ]");
    }

    private String getReleaseCandidateOrFinal(String env) {

        switch (env) {
            case "dev":
                return "-SNAPSHOT";
            case "stage":
                return "-rc";
            case "prod":
            case "production":
                return "";
            default:
                return "-" + env;
        }
    }

    private String getContainerTag(String env) {

        if (StringUtils.equalsIgnoreCase(env, "prod")) {
            return "$(cat version).${CI_COMMIT_SHORT_SHA}";
        }

        return "$(cat version)";
    }
}
