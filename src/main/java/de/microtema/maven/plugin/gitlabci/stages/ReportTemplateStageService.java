package de.microtema.maven.plugin.gitlabci.stages;

import de.microtema.maven.plugin.gitlabci.PipelineGeneratorMojo;
import de.microtema.maven.plugin.gitlabci.PipelineGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ReportTemplateStageService implements TemplateStageService {

    private final TestReportTemplateStageService testReportTemplateStageService;

    private final ChangeLogTemplateStageService changeLogTemplateStageService;

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        if (!PipelineGeneratorUtil.hasSourceCode(project)) {
            return null;
        }

        String template = getStagesTemplate(mojo, project);

        return PipelineGeneratorUtil.trimEmptyLines(template);
    }

    private String getStagesTemplate(PipelineGeneratorMojo mojo, MavenProject project) {

        return Stream.of(testReportTemplateStageService, changeLogTemplateStageService)
                .map(it -> it.getTemplate(mojo, project))
                .filter(Objects::nonNull)
                .map(PipelineGeneratorUtil::trimEmptyLines)
                .collect(Collectors.joining("\n\n"));
    }
}
