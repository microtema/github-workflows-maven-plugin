package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.stream.Stream;

public class SecurityTemplateStageService implements TemplateStageService {

    private final BlackDuckScanTemplateStageService blackDuckScanTemplateStageService;

    private final DependencyCheckTemplateStageService dependencyCheckTemplateStageService;

    public SecurityTemplateStageService(BlackDuckScanTemplateStageService blackDuckScanTemplateStageService,
                                        DependencyCheckTemplateStageService dependencyCheckTemplateStageService) {
        this.blackDuckScanTemplateStageService = blackDuckScanTemplateStageService;

        this.dependencyCheckTemplateStageService = dependencyCheckTemplateStageService;
    }

    @Override
    public String getJobId() {

        return "security-check";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return Stream.of(blackDuckScanTemplateStageService, dependencyCheckTemplateStageService).anyMatch(it -> it.access(mojo, metaData));
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (blackDuckScanTemplateStageService.access(mojo, metaData)) {

            return blackDuckScanTemplateStageService.getTemplate(mojo, metaData);
        }

        return dependencyCheckTemplateStageService.getTemplate(mojo, metaData);
    }
}
