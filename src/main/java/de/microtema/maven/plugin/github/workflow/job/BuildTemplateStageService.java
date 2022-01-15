package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

public class BuildTemplateStageService implements TemplateStageService {

    private final SonarTemplateStageService sonarTemplateStageService;
    private final UnitTestTemplateStageService unitTestTemplateStageService;
    private final IntegrationTestTemplateStageService itTestTemplateStageService;

    public BuildTemplateStageService(SonarTemplateStageService sonarTemplateStageService,
                                     UnitTestTemplateStageService unitTestTemplateStageService,
                                     IntegrationTestTemplateStageService itTestTemplateStageService) {
        this.sonarTemplateStageService = sonarTemplateStageService;
        this.unitTestTemplateStageService = unitTestTemplateStageService;
        this.itTestTemplateStageService = itTestTemplateStageService;
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        MavenProject project = mojo.getProject();

        if (!PipelineGeneratorUtil.hasSourceCode(project)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        List<String> needs = new ArrayList<>();

        if (sonarTemplateStageService.access(mojo, metaData)) {

            needs.add("quality-gate");
        } else {

            if (unitTestTemplateStageService.access(mojo, metaData)) {
                needs.add("unit-test");
            }

            if (itTestTemplateStageService.access(mojo, metaData)) {
                needs.add("it-test");
            }
        }

        if (needs.isEmpty()) {

            needs.add("compile");
        }

        return template.replaceFirst("%NEEDS%", String.join(", ", String.join(", ", needs)))
                .replace("%POM_ARTIFACT%", "true");
    }
}
