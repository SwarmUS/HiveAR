package com.swarmus.hivear.models;

import java.util.ArrayList;
import java.util.List;

public class Robot {

    private String name;
    private int uid;
    private List<FunctionTemplate> commands;

    public Robot(String name, int uid) {
        this(name, uid, new ArrayList<>());
    }

    public Robot(String name, int uid, List<FunctionTemplate> commands) {
        this.name = name;
        this.uid = uid;
        this.commands = commands;
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

    public List<FunctionTemplate> getCommands() {
        return commands;
    }

    public void setCommands(List<FunctionTemplate> commands) {
        this.commands = commands;
    }

    public void addCommand(FunctionTemplate command) {this.commands.add(command);}

    public void clearCommands() {this.commands.clear();}
}
