package de.microtema.maven.plugin.github.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class MetaData {

    private String branchName;

    private String branchFillName;

    private String branchPattern;

    private String stageName;

    private List<String> stageNames = new ArrayList<>();

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getBranchPattern() {
        return branchPattern;
    }

    public void setBranchPattern(String branchPattern) {
        this.branchPattern = branchPattern;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public List<String> getStageNames() {
        return stageNames;
    }

    public void setStageNames(List<String> stageNames) {
        this.stageNames = stageNames;
    }

    public String getBranchFullName() {
        return branchFillName;
    }

    public void setBranchFullName(String branchFillName) {
        this.branchFillName = branchFillName;
    }
}
