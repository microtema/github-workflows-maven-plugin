package de.microtema.maven.plugin.github.workflow.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Service {

    private String name;

    private List<String> command = new ArrayList<>();
}
