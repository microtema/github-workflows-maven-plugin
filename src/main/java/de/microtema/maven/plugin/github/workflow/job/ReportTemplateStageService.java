package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ReportTemplateStageService implements TemplateStageService {

    private final TestReportTemplateStageService testReportTemplateStageService;

    private final ChangeLogTemplateStageService changeLogTemplateStageService;

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.hasSourceCode(mojo.getProject())) {
            return null;
        }

        String template = getStagesTemplate(mojo, metaData);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    private String getStagesTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        return Stream.of(testReportTemplateStageService, changeLogTemplateStageService)
                .map(it -> it.getTemplate(mojo, metaData))
                .filter(Objects::nonNull)
                .map(PipelineGeneratorUtil::trimEmptyLines)
                .collect(Collectors.joining("\n\n"));
    }
}
