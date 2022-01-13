package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.List;

public class SonarTemplateStageService implements TemplateStageService {

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.hasSourceCode(mojo.getProject())) {
            return null;
        }

        if (!PipelineGeneratorUtil.hasSonarProperties(mojo.getProject())) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        List<String> sonarExcludes = PipelineGeneratorUtil.getSonarExcludes(mojo.getProject());

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
