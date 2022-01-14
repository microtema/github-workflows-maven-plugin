package de.microtema.maven.plugin.github.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectCache {

    private String key;

    private List<String> paths = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
