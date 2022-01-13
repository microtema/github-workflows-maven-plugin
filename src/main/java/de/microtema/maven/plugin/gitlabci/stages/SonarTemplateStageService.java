package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import org.apache.maven.project.MavenProject;

import java.util.List;

public class SonarTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.hasSourceCode(project)) {
            return null;
        }

        if (!PipelineGeneratorUtil.hasSonarProperties(project)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        List<String> sonarExcludes = PipelineGeneratorUtil.getSonarExcludes(project);

        if (sonarExcludes.isEmpty()) {
            return template;
        }

        StringBuilder excludes = new StringBuilder(" -pl ");

        for (int index = 0; index < sonarExcludes.size(); index++) {

            String exclude = sonarExcludes.get(index);

            excludes.append("!").append(exclude);

            if (index < sonarExcludes.size() - 1) {
                excludes.append(" ");
            }
        }

        return template.replaceFirst("sonar:sonar", "sonar:sonar" + excludes);
    }
}
