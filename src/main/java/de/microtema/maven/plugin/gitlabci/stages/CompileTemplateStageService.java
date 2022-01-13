package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;

@RequiredArgsConstructor
public class CompileTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.hasSourceCode(project)) {
            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getName());
    }
}
