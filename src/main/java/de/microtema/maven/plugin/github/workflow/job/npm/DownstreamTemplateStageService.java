package de.microtema.maven.plugin.github.workflow.job.npm;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.job.TemplateStageService;
import de.microtema.maven.plugin.github.workflow.model.JobData;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DownstreamTemplateStageService implements TemplateStageService {

    private final List<TemplateStageService> multipleStageTemplateStageServices = new ArrayList<>();
    private final List<TemplateStageService> singleStageTemplateStageServices = new ArrayList<>();

    private final DeploymentTemplateStageService deploymentTemplateStageService;

    public DownstreamTemplateStageService(
            BuildTemplateStageService buildTestTemplateStageService,
            DeploymentTemplateStageService deploymentTemplateStageService,
            ReadinessTemplateStageService readinessTemplateStageService) {

        this.deploymentTemplateStageService = deploymentTemplateStageService;

        multipleStageTemplateStageServices.add(readinessTemplateStageService);

        singleStageTemplateStageServices.add(readinessTemplateStageService);
        singleStageTemplateStageServices.add(buildTestTemplateStageService);
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (Objects.isNull(mojo.getDownStreams())) {
            return false;
        }

        List<String> stageNames = metaData.getStageNames();

        return mojo.getDownStreams().entrySet().stream().anyMatch(it -> stageNames.contains(it.getKey()));
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        List<String> stageNames = metaData.getStageNames();

        Map<String, String> downStreams = mojo.getDownStreams();

        boolean multipleStages = stageNames.size() > 1;

        return stageNames.stream().filter(downStreams::containsKey).map(it -> {

            String defaultTemplate = PipelineGeneratorUtil.getTemplate(getTemplateName());

            String needs = getJobNames(mojo, metaData, it);

            String downStream = downStreams.get(it);

            String[] parts = downStream.split(":");

            String workflowName = parts[0];
            String downstreamRepository = "${{ github.repository }}";

            if (parts.length > 1) {
                downstreamRepository = parts[1];
            }

            JobData jobData = getJobData(parts, it);

            String jobId = jobData.getId();
            String jobName = jobData.getName();

            HashMap<String, Object> globalVariables = new HashMap<>(mojo.getVariables());
            globalVariables.put("DOWNSTREAM_REPOSITORY", downstreamRepository);

            defaultTemplate = PipelineGeneratorUtil.applyProperties(defaultTemplate, it, globalVariables);

            return defaultTemplate
                    .replace("downstream:", multipleStages ? "downstream-" + jobId.toLowerCase() + ":" : "downstream:")
                    .replace("%JOB_NAME%", jobName)
                    .replace("%WORKFLOW%", workflowName)
                    .replace("%NEEDS%", needs);

        }).collect(Collectors.joining("\n"));
    }

    @Override
    public String getJobIds(MetaData metaData, String stageName) {

        List<String> stageNames = metaData.getStageNames();

        String jobName = getJobId();

        if (CollectionUtils.size(stageNames) == 1) {
            return jobName;
        }

        Map<String, String> downStreams = metaData.getDownStreams();

        return stageNames.stream()
                .filter(downStreams::containsKey)
                .filter(it -> StringUtils.equalsIgnoreCase(it, stageName))
                .map(it -> {
                    String downStream = downStreams.get(it);

                    String[] parts = downStream.split(":");

                    JobData jobData = getJobData(parts, it);

                    return ("downstream-" + jobData.getId()).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }

    private String getJobNames(PipelineGeneratorMojo mojo, MetaData metaData, String stageName) {

        boolean microserviceRepo = PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject());

        String needs;

        if (microserviceRepo) {

            needs = multipleStageTemplateStageServices.stream()
                    .filter(t -> t.access(mojo, metaData))
                    .map(t -> t.getJobIds(metaData, stageName))
                    .findFirst().orElse(StringUtils.EMPTY);

            if (StringUtils.isEmpty(needs)) {
                needs = deploymentTemplateStageService.getJobIds(metaData, stageName);
            }

            return needs;
        }

        return singleStageTemplateStageServices.stream()
                .filter(t -> t.access(mojo, metaData))
                .map(TemplateStageService::getJobName)
                .findFirst().orElseThrow(() -> new IllegalStateException("Unable to get job name for stage: " + stageName));
    }

    private JobData getJobData(String[] parts, String stageName) {

        String jobId = stageName;
        String jobName = "Downstream";

        if (parts.length > 1) {
            jobId += "-" + parts[0].replaceAll("[^a-zA-Z0-9]", "-");
            jobName = parts[0];
        }

        jobId = Stream.of(jobId.split("-")).map(StringUtils::trimToNull).filter(Objects::nonNull).collect(Collectors.joining("-"));

        JobData jobData = new JobData();

        jobData.setId(jobId);
        jobData.setName(jobName);

        return jobData;
    }
}
