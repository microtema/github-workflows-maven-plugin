package de.microtema.maven.plugin.github.workflow.job.npm;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.job.DownstreamTemplateStageService;
import de.microtema.maven.plugin.github.workflow.job.ReadinessTemplateStageService;
import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NotificationTemplateStageService implements TemplateStageService {

    private static final String WEBHOOK_URL = "NOTIFICATION_WEBHOOK_URL";

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public NotificationTemplateStageService(DownstreamTemplateStageService downstreamTemplateStageService,
                                            ReadinessTemplateStageService readinessTemplateStageService) {
        this.templateStageServices.add(downstreamTemplateStageService);
        this.templateStageServices.add(readinessTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Stream.of("feature", "bugfix").anyMatch(it -> StringUtils.equalsIgnoreCase(metaData.getBranchName(), it))) {
            return false;
        }

        return metaData.getStageNames().stream()
                .map(PipelineGeneratorUtil::findProperties)
                .filter(Objects::nonNull)
                .map(it -> it.getProperty(WEBHOOK_URL))
                .anyMatch(StringUtils::isNotEmpty);
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        boolean multipleStages = stageNames.size() > 1;

        return stageNames.stream().map(it -> {

                    Properties properties = PipelineGeneratorUtil.findProperties(it);

                    String notificationWebhookUrl = Optional.ofNullable(properties).map(p -> p.getProperty(WEBHOOK_URL)).orElse(null);

                    if (StringUtils.isEmpty(notificationWebhookUrl)) {
                        return null;
                    }

                    String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

                    defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it);

                    String needs = templateStageServices.stream()
                            .filter(e -> e.access(mojo, metaData))
                            .map(e -> e.getJobIds(metaData, it))
                            .map(String::valueOf)
                            .filter(StringUtils::isNotEmpty)
                            .findFirst().orElseThrow(() -> new NoSuchElementException("Unable to find the job id!"));

                    return defaultTemplate
                            .replace("notification:", multipleStages ? "notification-" + it.toLowerCase() + ":" : "notification:")
                            .replace("%JOB_NAME%", PipelineGeneratorUtil.getJobName("Notification", it, multipleStages))
                            .replace("%" + WEBHOOK_URL + "%", notificationWebhookUrl)
                            .replace("%STAGE_DISPLAY_NAME%", it.toUpperCase())
                            .replace("%NEEDS%", needs);

                }).filter(Objects::nonNull)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
