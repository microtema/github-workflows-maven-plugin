package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;

@RequiredArgsConstructor
public class PublishTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (PipelineGeneratorUtil.existsDockerfile(project)) {
            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getName());
    }
}
