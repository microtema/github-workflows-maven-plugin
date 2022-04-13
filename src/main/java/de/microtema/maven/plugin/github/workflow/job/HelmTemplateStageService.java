package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class HelmTemplateStageService implements TemplateStageService {

    private final DbMigrationTemplateStageService dbMigrationTemplateStageService;
    private final PackageTemplateStageService packageTemplateStageService;
    private final TagTemplateStageService tagTemplateStageService;

    public HelmTemplateStageService(DbMigrationTemplateStageService dbMigrationTemplateStageService,
                                    PackageTemplateStageService packageTemplateStageService,
                                    TagTemplateStageService tagTemplateStageService) {
        this.dbMigrationTemplateStageService = dbMigrationTemplateStageService;
        this.packageTemplateStageService = packageTemplateStageService;
        this.tagTemplateStageService = tagTemplateStageService;
    }

    @Override
    public String getJobId() {
        return "deployment";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "feature")) {
            return false;
        }

        if (StringUtils.equalsIgnoreCase(metaData.getBranchName(), "bugfix")) {
            return false;
        }

        return PipelineGeneratorUtil.existsHelmFile(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean masterBranch = StringUtils.equalsIgnoreCase(metaData.getBranchName(), "master");

        boolean multipleStages = stageNames.size() > 1;
        boolean sameDockerRegistry = PipelineGeneratorUtil.isSameDockerRegistry(stageNames);

        return stageNames.stream().map(it -> {

            String template = PipelineGeneratorUtil.getTemplate(getTemplateName());

            template = PipelineGeneratorUtil.applyProperties(template, it);

            String needs = getJobNeeds(mojo, metaData, it, sameDockerRegistry);

            if (StringUtils.isNotEmpty(needs)) {

                template = template.replaceAll("%NEEDS%", needs);
            } else {

                template = template.replaceAll("\\[ %NEEDS% \\]", "[ ]");
            }

            return template
                    .replace("deployment:", multipleStages ? "deployment-" + it.toLowerCase() + ":" : "deployment:")
                    .replace("%JOB_NAME%", "[" + it.toUpperCase() + "] Deployment")
                    .replace("$VERSION.$SHORT_SHA", masterBranch ? "$VERSION.$SHORT_SHA" : "$VERSION")
                    .replace("%STAGE_NAME%", it);

        }).collect(Collectors.joining());
    }

    private String getJobNeeds(PipelineGeneratorMojo mojo, MetaData metaData, String stageName, boolean sameDockerRegistry) {

        if (dbMigrationTemplateStageService.access(mojo, metaData)) {

            if (sameDockerRegistry) {
                return dbMigrationTemplateStageService.getJobId();
            }

            return dbMigrationTemplateStageService.getJobIds(metaData, stageName);
        }

        if (tagTemplateStageService.access(mojo, metaData)) {

            return tagTemplateStageService.getJobId();
        }

        if (packageTemplateStageService.access(mojo, metaData)) {

            if (sameDockerRegistry) {
                return packageTemplateStageService.getJobId();
            }

            return packageTemplateStageService.getJobIds(metaData, stageName);
        }

        return null;
    }
}
