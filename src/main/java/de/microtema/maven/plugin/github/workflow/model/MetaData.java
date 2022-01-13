package de.microtema.maven.plugin.github.workflow.model;

import lombok.Data;

@Data
public class MetaData {

    private String branchName;

    private String branchPattern;

    private String stageName;
}
