package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LiquibaseTemplateStageService implements TemplateStageService {

    private final PackageTemplateStageService packageTemplateStageService;

    public LiquibaseTemplateStageService(PackageTemplateStageService packageTemplateStageService) {
        this.packageTemplateStageService = packageTemplateStageService;
    }

    @Override
    public String getJobId() {
        return "db-migration";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
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

            String needs = packageTemplateStageService.getJobId();

            return defaultTemplate
                    .replace("db-migration:", multipleStages ? "db-migration-" + it.toLowerCase() + ":" : "db-migration:")
                    .replace("%JOB_NAME%", multipleStages ? "Database Changelog [" + it.toUpperCase() + "]" : "Database Changelog")
                    .replace("%NEEDS%", needs);

        }).collect(Collectors.joining("\n"));
    }
}
