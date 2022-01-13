package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class VersioningTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (StringUtils.equals(metaData.getBranchName(), "develop")) {

            return null;
        }

        if (StringUtils.equals(metaData.getBranchName(), "feature")) {

            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getName()).replace("[ %NEEDS% ]", "[ ]");
    }
}
