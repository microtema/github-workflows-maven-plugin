package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import org.apache.maven.project.MavenProject;

import java.util.List;

public class TestReportTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.hasSourceCode(project)) {
            return null;
        }

        return PipelineGeneratorUtil.getTemplate(getName());
    }
}
