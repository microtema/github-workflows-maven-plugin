package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.ArrayList;
import java.util.List;

public class BuildTemplateStageService implements TemplateStageService {

    private final VersioningTemplateStageService versioningTemplateStageService;
    private final SonarTemplateStageService sonarTemplateStageService;
    private final UnitTestTemplateStageService unitTestTemplateStageService;
    private final IntegrationTestTemplateStageService itTestTemplateStageService;
    private final SecurityTemplateStageService securityTemplateStageService;
    private final CompileTemplateStageService compileTemplateStageService;

    public BuildTemplateStageService(
            VersioningTemplateStageService versioningTemplateStageService,
            SonarTemplateStageService sonarTemplateStageService,
            UnitTestTemplateStageService unitTestTemplateStageService,
            IntegrationTestTemplateStageService itTestTemplateStageService,
            SecurityTemplateStageService securityTemplateStageService,
            CompileTemplateStageService compileTemplateStageService) {
        this.versioningTemplateStageService = versioningTemplateStageService;
        this.sonarTemplateStageService = sonarTemplateStageService;
        this.unitTestTemplateStageService = unitTestTemplateStageService;
        this.itTestTemplateStageService = itTestTemplateStageService;
        this.securityTemplateStageService = securityTemplateStageService;
        this.compileTemplateStageService = compileTemplateStageService;
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        return versioningTemplateStageService.access(mojo, metaData);
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        List<String> needs = new ArrayList<>();

        if (sonarTemplateStageService.access(mojo, metaData)) {

            needs.add(sonarTemplateStageService.getJobName());

            if (securityTemplateStageService.access(mojo, metaData)) {
                needs.add(securityTemplateStageService.getJobName());
            }
        } else {

            if (unitTestTemplateStageService.access(mojo, metaData)) {
                needs.add(unitTestTemplateStageService.getJobName());
            }

            if (itTestTemplateStageService.access(mojo, metaData)) {
                needs.add(itTestTemplateStageService.getJobName());
            }
        }

        if (needs.isEmpty()) {

            needs.add(compileTemplateStageService.getJobName());
        }

        if (!PipelineGeneratorUtil.isMicroserviceRepo(mojo.getProject())) {

            template = template.replace("mvn package", "mvn install");
        }

        return template.replaceFirst("%NEEDS%", String.join(", ", String.join(", ", needs)))
                .replace("%POM_ARTIFACT%", "true");
    }
}
