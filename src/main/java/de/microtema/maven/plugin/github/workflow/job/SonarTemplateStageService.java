package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SonarTemplateStageService implements TemplateStageService {

    @Override
    public String getJobName() {
        return "quality-gate";
    }

    @Override
    public boolean access(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.hasSourceCode(mojo.getProject())) {
            return false;
        }

        return PipelineGeneratorUtil.hasSonarProperties(mojo.getProject());
    }

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!access(mojo, metaData)) {
            return null;
        }

        String template = PipelineGeneratorUtil.getTemplate(getName());

        List<String> sonarExcludes = PipelineGeneratorUtil.getSonarExcludes(mojo.getProject());

        List<String> needs = new ArrayList<>();
        needs.add("unit-test");

        if (PipelineGeneratorUtil.existsIntegrationTests(mojo.getProject())) {
            needs.add("it-test");
        }

        String branchNameSupport = PipelineGeneratorUtil.getProperty(mojo.getProject(), "sonar.branch.name.support", "false");

        if (StringUtils.equalsIgnoreCase(branchNameSupport, "true")) {

            template = template.replace("$SONAR_TOKEN", "$SONAR_TOKEN -Dsonar.branch.name=${GITHUB_REF##*/}");
        }

        template = template.replaceFirst("%NEEDS%", String.join(", ", needs));

        if (sonarExcludes.isEmpty()) {
            return template;
        }

        StringBuilder excludes = new StringBuilder(" -pl ");

        for (int index = 0; index < sonarExcludes.size(); index++) {

            String exclude = sonarExcludes.get(index);

            excludes.append("!").append(exclude);

            if (index < sonarExcludes.size() - 1) {
                excludes.append(" ");
            }
        }

        return template.replaceFirst("sonar:sonar", "sonar:sonar" + excludes);
    }
}
