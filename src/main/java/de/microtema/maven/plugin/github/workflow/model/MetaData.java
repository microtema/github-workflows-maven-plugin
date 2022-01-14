package de.microtema.maven.plugin.github.workflow.model;

public class MetaData {

    private String branchName;

    private String branchPattern;

    private String stageName;

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
}
