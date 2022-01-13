package de.microtema.maven.plugin.gitlabci.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Service {

    private String name;

    private List<String> command = new ArrayList<>();
}
