package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LiquibaseTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public LiquibaseTemplateStageService(TagTemplateStageService tagTemplateStageService, PackageTemplateStageService packageTemplateStageService) {
        this.templateStageServices.add(tagTemplateStageService);
        this.templateStageServices.add(packageTemplateStageService);
    }

    @Override
    public String getJobId() {
        return "db-migration";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!metaData.isDeployable()) {
            return false;
        }

        return PipelineGeneratorUtil.existsLiquibase(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean multipleStages = stageNames.size() > 1;

        return stageNames.stream().map(it -> {

            String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it);

            String needs = templateStageServices.stream()
                    .filter(t -> t.access(mojo, metaData))
                    .findFirst().map(t -> t.getJobIds(metaData, it))
                    .orElse("build");

            return defaultTemplate
                    .replace("db-migration:", multipleStages ? "db-migration-" + it.toLowerCase() + ":" : "db-migration:")
                    .replace("%JOB_NAME%", PipelineGeneratorUtil.getJobName("Database Changelog", it, multipleStages))
                    .replace("%NEEDS%", needs)
                    .replace("%STAGE_NAME%", it);

        }).collect(Collectors.joining("\n"));
    }
}
