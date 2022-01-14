package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class VersioningTemplateStageService implements TemplateStageService {

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return Stream.of("develop", "feature").noneMatch(it -> StringUtils.equalsIgnoreCase(it, metaData.getBranchName()));
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {

            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName()).replace("[ %NEEDS% ]", "[ ]");

        if (StringUtils.equals(metaData.getBranchName(), "master")) {

            template = template.replace("{POM_VERSION/-SNAPSHOT/-RC}", "{POM_VERSION/-SNAPSHOT/}");
        }

        return template;
    }
}
