package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        String template = PipelineGeneratorUtil.getTemplate(getTemplateName());

        List<String> needs = new ArrayList<>();

        if (sonarTemplateStageService.access(mojo, metaData)) {

            needs.add(sonarTemplateStageService.getJobId());

            if (securityTemplateStageService.access(mojo, metaData)) {
                needs.add(securityTemplateStageService.getJobId());
            }
        } else {

            if (unitTestTemplateStageService.access(mojo, metaData)) {
                needs.add(unitTestTemplateStageService.getJobId());
            }

            if (itTestTemplateStageService.access(mojo, metaData)) {
                needs.add(itTestTemplateStageService.getJobId());
            }
        }

        if (needs.isEmpty()) {

            needs.add(compileTemplateStageService.getJobId());
        }

        String mkdirCommand = "mkdir -p artifact/target";
        String copyCommand = "cp target/*.jar artifact/target/";

        List<String> modules = new ArrayList<>(mojo.getProject().getModules());

        if (PipelineGeneratorUtil.isMavenPomRepo(mojo.getProject())) {
            copyCommand = "cp -r target/* artifact/target/";
        }

        if (!modules.isEmpty()) {
            String padding = "        ";
            mkdirCommand = modules.stream().map(it -> "mkdir -p artifact/" + it + "/target").collect(Collectors.joining(System.lineSeparator() + padding));
            copyCommand = modules.stream().map(it -> "cp " + it + "/target/*.jar artifact/" + it + "/target/").collect(Collectors.joining(System.lineSeparator() + padding));
        } else if (PipelineGeneratorUtil.isMavenPomRepo(mojo.getProject())) {
            copyCommand = "cp pom.xml artifact/target/";
        }

        return template.replaceFirst("%NEEDS%", String.join(", ", String.join(", ", needs)))
                .replace("%MKDIR_COMMAND%", mkdirCommand)
                .replace("%COPY_COMMAND%", copyCommand)
                .replace("%POM_ARTIFACT%", "'true'");
    }
}
