package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.ArrayList;
import java.util.List;

public class DbMigrationTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public DbMigrationTemplateStageService(LiquibaseTemplateStageService liquibaseTemplateStageService,
                                           FlywayTemplateStageService flywayTemplateStageService) {
        templateStageServices.add(liquibaseTemplateStageService);
        templateStageServices.add(flywayTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return templateStageServices.stream().anyMatch(it -> it.access(mojo, metaData));
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        return templateStageServices.stream()
                .filter(it -> it.access(mojo, metaData))
                .findFirst().map(it -> it.getTemplate(mojo, metaData))
                .orElse(null);
    }
}
