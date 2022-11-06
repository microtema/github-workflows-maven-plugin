package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnitTestTemplateStageService implements TemplateStageService {

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (PipelineGeneratorUtil.isSpeedBranch(metaData.getBranchName())) {
            return false;
        }

        return PipelineGeneratorUtil.existsUnitTests(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getTemplateName());

        String mkdirCommand = "mkdir -p artifact/target/surefire-reports";
        String copyCommand = "cp -r target/surefire-reports/* artifact/target/surefire-reports/";
        String copyFileCommand = "cp -r target/jacoco.exec artifact/target/surefire-reports/";

        List<String> modules = new ArrayList<>(mojo.getProject().getModules());

        if (!modules.isEmpty()) {
            String padding = "        ";
            copyCommand = modules.stream().map(it -> "cp -r " + it + "/target/surefire-reports/* artifact/target/surefire-reports/").collect(Collectors.joining(System.lineSeparator() + padding));
            copyFileCommand = modules.stream().map(it -> "mv " + it + "/target/jacoco.exec artifact/target/surefire-reports/" + it + ".exec").collect(Collectors.joining(System.lineSeparator() + padding));
        } else if (PipelineGeneratorUtil.isMavenPomRepo(mojo.getProject())) {
            copyCommand = "echo no surefire-reports to copy";
            copyFileCommand = "echo no jacoco.exec to copy";
        }

        return template.replace("%MKDIR_COMMAND%", mkdirCommand)
                .replace("%COPY_COMMAND%", copyCommand)
                .replace("%COPY_FILE_COMMAND%", copyFileCommand);
    }
}
