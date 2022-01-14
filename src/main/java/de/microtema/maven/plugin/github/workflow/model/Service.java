package de.microtema.maven.plugin.github.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class Service {

    private String name;

    private List<String> command = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }
}
