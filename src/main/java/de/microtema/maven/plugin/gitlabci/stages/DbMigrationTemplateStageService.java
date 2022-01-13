package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import org.apache.maven.project.MavenProject;

import java.util.List;

public class DbMigrationTemplateStageService implements TemplateStageService {

    @Override
    public String getName() {
        return "database-migration";
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.existsDbMigrationScripts(project)) {
            return null;
        }

        String template = getStagesTemplate(mojo);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    @Override
    public String getTemplate(String env, List<String> branches) {

        return PipelineGeneratorUtil.getTemplate(getName())
                .replace("%STAGE_DISPLAY_NAME%", "DB Migration:" + env.toUpperCase())
                .replace("%PROFILE_NAME%", "db-migration-" + env.toLowerCase())
                .replace("%REFS%", "[ " + String.join(", ", branches) + " ]");
    }
}
