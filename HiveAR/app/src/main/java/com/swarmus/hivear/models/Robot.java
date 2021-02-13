package com.swarmus.hivear.models;

import java.util.Arrays;
import java.util.List;

public class Robot {

    private String name;
    private int uid;
    private List<String> commands;

    private boolean expandable;

    public Robot(String name, int uid) {
        this.name = name;
        this.uid = uid;
        this.expandable = false;
        // TODO remove after examples
        commands = Arrays.asList("Command 1", "Command 2", "Command 3");
    }

    @Override
    public String toString() {
        return "Robot{" +
                "name='" + name + '\'' +
                ", uid=" + uid +
                '}';
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
