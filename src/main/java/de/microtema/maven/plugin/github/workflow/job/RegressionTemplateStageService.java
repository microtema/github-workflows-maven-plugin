package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class RegressionTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsDockerfile(mojo.getProject())) {
            return null;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
            return null;
        }

        String template = getStagesTemplate(mojo, metaData);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    private String getStagesTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        List<String> regressionTestTypes = PipelineGeneratorUtil.getRegressionTestTypes(mojo.getProject());

        regressionTestTypes.removeIf(it -> !PipelineGeneratorUtil.existsRegressionTests(mojo.getProject(), it));

        if (regressionTestTypes.isEmpty()) {
            return null;
        }

        return regressionTestTypes.stream().map(f -> getTemplate(f, metaData.getStageName())).collect(Collectors.joining("\n"));
    }

    private String getTemplate(String testType, String env) {

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%PROFILE_NAME%", testType.toLowerCase())
                .replace("%STAGE_NAME%", env.toLowerCase());
    }
}
