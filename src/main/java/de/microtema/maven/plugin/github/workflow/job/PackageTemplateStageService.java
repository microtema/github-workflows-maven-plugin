package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageTemplateStageService implements TemplateStageService {

    private final BuildTemplateStageService buildTemplateStageService;

    public PackageTemplateStageService(BuildTemplateStageService buildTemplateStageService) {
        this.buildTemplateStageService = buildTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean multipleStages = stageNames.size() > 1;
        boolean sameDockerRegistry = PipelineGeneratorUtil.isSameDockerRegistry(stageNames);
        boolean masterBranch = StringUtils.equalsIgnoreCase(metaData.getBranchName(), "master");

        String dockerTag = masterBranch ? "$VERSION.$SHORT_SHA" : "$VERSION";

        if (!multipleStages || sameDockerRegistry) {

            String defaultTemplate = getSpecificTemplate(metaData.getStageName());

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, metaData.getStageName());

            return getTemplate(defaultTemplate, "package", "Package", dockerTag);
        }

        return stageNames.stream().map(it -> {

            String defaultTemplate = getSpecificTemplate(it);

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it, mojo.getVariables());

            String jobName = PipelineGeneratorUtil.getJobName("Package", it, multipleStages);

            return getTemplate(defaultTemplate, "package-" + it.toLowerCase(), jobName, dockerTag);

        }).collect(Collectors.joining("\n"));
    }

    private String getSpecificTemplate(String stageName) {

        Properties properties = PipelineGeneratorUtil.findProperties(stageName);

        if (Objects.isNull(properties)) {

            return PipelineGeneratorUtil.getTemplate("docker-package");
        }

        String accessKeyId = properties.getProperty("AWS_ACCESS_KEY_ID", null);

        if (Objects.nonNull(accessKeyId)) {
            return PipelineGeneratorUtil.getTemplate("ecr-docker-package");
        }

        return PipelineGeneratorUtil.getTemplate("docker-package");
    }

    private String getTemplate(String template, String jobId, String jobName, String imageTag) {

        String needs = buildTemplateStageService.getJobId();

        return template
                .replace("package:", jobId + ":")
                .replace("%JOB_NAME%", jobName)
                .replace("%NEEDS%", needs)
                .replace("$VERSION.$SHORT_SHA", imageTag);
    }
}
