package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class NotificationTemplateStageService implements TemplateStageService {

    private static final String WEBHOOK_URL = "NOTIFICATION_WEBHOOK_URL";

    private final List<TemplateStageService> templateStageServices = new ArrayList<>();

    public NotificationTemplateStageService(DownstreamTemplateStageService downstreamTemplateStageService,
                                            PerformanceTestTemplateStageService performanceTestTemplateStageService,
                                            SystemTestTemplateStageService systemTestTemplateStageService,
                                            ReadinessTemplateStageService readinessTemplateStageService,
                                            UnDeploymentTemplateStageService unDeploymentTemplateStageService) {
        this.templateStageServices.add(downstreamTemplateStageService);
        this.templateStageServices.add(performanceTestTemplateStageService);
        this.templateStageServices.add(systemTestTemplateStageService);
        this.templateStageServices.add(readinessTemplateStageService);
        this.templateStageServices.add(unDeploymentTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!metaData.isDeployable()) {
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
                            .collect(Collectors.joining(", "));

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
