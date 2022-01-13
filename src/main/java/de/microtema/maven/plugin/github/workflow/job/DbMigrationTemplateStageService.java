package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.List;

public class DbMigrationTemplateStageService implements TemplateStageService {

    @Override
    public String getName() {
        return "database-migration";
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.existsDbMigrationScripts(mojo.getProject())) {
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
