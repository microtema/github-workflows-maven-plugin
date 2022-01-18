package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class HelmTemplateStageService implements TemplateStageService {

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
            return false;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "bugfix")) {
            return false;
        }

        return PipelineGeneratorUtil.existsHelmFile(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "develop")) {
            template = template.replaceFirst("environment: %STAGE_NAME%", StringUtils.EMPTY);
        }

        return template.replaceFirst("%STAGE_NAME%", metaData.getStageName().toLowerCase());
    }
}
