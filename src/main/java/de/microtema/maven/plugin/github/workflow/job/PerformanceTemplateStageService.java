package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class PerformanceTemplateStageService implements TemplateStageService {

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "master")) {
            return false;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
            return false;
        }

        return PipelineGeneratorUtil.existsPerformanceTests(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        String stageName = metaData.getStageName();

        String needs = "readiness";

        return template
                .replace("%PROFILE_NAME%", stageName.toLowerCase())
                .replace("%NEEDS%", needs)
                .replace("%STAGE_NAME%", stageName.toLowerCase());
    }
}
