package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class CompileTemplateStageService implements TemplateStageService {

    private final VersioningTemplateStageService versioningTemplateStageService;

    @Override
    public String getTemplate(PipelineGeneratorMojo mojo, MetaData metaData) {

        if (!PipelineGeneratorUtil.hasSourceCode(mojo.getProject())) {
            return null;
        }

        // NOTE: check if the branch is develop for the first job
        boolean develop = StringUtils.equalsIgnoreCase("develop", metaData.getBranchName());

        String template = PipelineGeneratorUtil.getTemplate(getName());

        if (develop) {
            return template.replace("[ %NEEDS% ]", "[ ]");
        }

        return template.replace("%NEEDS%", versioningTemplateStageService.getName());
    }
}
