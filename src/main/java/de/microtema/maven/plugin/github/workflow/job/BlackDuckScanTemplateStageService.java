package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

public class BlackDuckScanTemplateStageService implements TemplateStageService {

    @Override
    public String getJobId() {
        return "security-check";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.hasSourceCode(mojo.getProject())) {
            return false;
        }

        String property = PipelineGeneratorUtil.getProperty(mojo.getProject(), "detect.project.name", "false");

        return !StringUtils.equalsIgnoreCase("false", property);
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        MavenProject project = mojo.getProject();

        return PipelineGeneratorUtil.getTemplate(getTemplateName())
                .replace("%blackduck.url%", PipelineGeneratorUtil.getProperty(project, "blackduck.url", "${{ secrets.BLACK_DUCK_URL }}"))
                .replace("%detect.project.name%", PipelineGeneratorUtil.getProperty(project, "detect.project.name", project.getArtifactId()))
                .replace("%detect.project.version.name%", PipelineGeneratorUtil.getProperty(project, "detect.project.version.name", project.getVersion()));
    }
}
