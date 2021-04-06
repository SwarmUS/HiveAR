package com.swarmus.hivear.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class Robot extends Observable {

    private String name;
    private int uid;
    private List<FunctionTemplate> commands;
    private List<FunctionTemplate> buzzCommands;

    private long lastUpdateTimeMillis;

    public Robot(String name, int uid) {
        this(name, uid, new ArrayList<>());
    }

    public Robot(String name, int uid, List<FunctionTemplate> commands) {
        lastUpdateTimeMillis = System.currentTimeMillis();
        this.name = name;
        this.uid = uid;
        this.commands = new ArrayList<>();
        this.buzzCommands = new ArrayList<>();
        setCommands(commands);
        this.addObserver((observable, o) -> {lastUpdateTimeMillis = System.currentTimeMillis();});
    }

    @Override
    public String toString() {
        return "Robot{" +
                "name='" + name + '\'' +
                ", uid=" + uid +
                '}';
    }

    public long getLastUpdateTimeMillis() {return lastUpdateTimeMillis;}

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
        notifyObservers();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyObservers();
    }

    public List<FunctionTemplate> getCommands() {
        return commands;
    }

    public List<FunctionTemplate> getBuzzCommands() {
        return buzzCommands;
    }

    public void setCommands(List<FunctionTemplate> commands) {
        // Iterate in list to classify if buzz or not
        for(FunctionTemplate functionTemplate : commands) {
            addCommand(functionTemplate);
        }
    }

    public void addCommand(FunctionTemplate command) {
        if (command.isBuzzFunction()) {
            this.buzzCommands.add(command);
        } else {
            this.commands.add(command);
        }
        notifyObservers();
    }

    public void clearCommands() {
        this.commands.clear();
        this.buzzCommands.clear();
        notifyObservers();
    }
}
