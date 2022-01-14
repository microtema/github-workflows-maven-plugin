package de.microtema.maven.plugin.github.workflow.job;

import de.microtema.maven.plugin.github.workflow.PipelineGeneratorMojo;
import de.microtema.maven.plugin.github.workflow.PipelineGeneratorUtil;
import de.microtema.maven.plugin.github.workflow.model.MetaData;
import org.apache.commons.lang3.StringUtils;

public class CompileTemplateStageService implements TemplateStageService {

    private final VersioningTemplateStageService versioningTemplateStageService;

    public CompileTemplateStageService(VersioningTemplateStageService versioningTemplateStageService) {
        this.versioningTemplateStageService = versioningTemplateStageService;
    }

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
