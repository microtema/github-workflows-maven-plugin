package de.microtema.maven.plugin.gitlabci.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectCache {

    private String key;

    private List<String> paths = new ArrayList<>();
}
