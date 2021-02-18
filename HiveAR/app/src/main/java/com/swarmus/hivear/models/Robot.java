package com.swarmus.hivear.models;

import java.util.Arrays;
import java.util.List;

public class Robot {

    private String name;
    private int uid;
    private List<String> commands;

    public Robot(String name, int uid) {
        this.name = name;
        this.uid = uid;
        // TODO remove after examples
        commands = Arrays.asList("moveBy");
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

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
