package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PackageTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsDockerfile(mojo.getProject())) {
            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getName());
    }
}
