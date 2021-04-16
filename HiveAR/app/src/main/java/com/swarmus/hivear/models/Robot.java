package com.swarmus.hivear.models;

import com.swarmus.hivear.MessageOuterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class Robot extends Observable {

    private String name;
    private int uid;

    private ProtoMsgStorer msgLogging;
    private ProtoMsgStorer sentCommands;

    private FunctionTemplateList commands;
    private FunctionTemplateList buzzCommands;

    private long lastUpdateTimeMillis;

    public Robot(String name, int uid) {
        this(name, uid, new ArrayList<>());
    }

    public Robot(String name, int uid, List<FunctionTemplate> commands) {
        lastUpdateTimeMillis = System.currentTimeMillis();
        this.name = name;
        this.uid = uid;

        msgLogging = new ProtoMsgStorer(5, getUniqueName());
        sentCommands = new ProtoMsgStorer(5, getUniqueName() + " Sent commands");

        this.commands = new FunctionTemplateList();
        this.commands.addObserver((observable, o) -> {
            setChanged();
            notifyObservers();
        });

        this.buzzCommands = new FunctionTemplateList();
        this.buzzCommands.addObserver(((observable, o) -> {
            setChanged();
            notifyObservers();
        }));
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
        setChanged();
        notifyObservers();
    }

    public String getName() {
        return name;
    }

    public String getUniqueName() {
        String uniqueName = name.isEmpty() ? "Agent" : name;
        return uniqueName + " #" + uid;
    }

    public void setName(String name) {
        this.name = name;
        setChanged();
        notifyObservers();
    }

    public FunctionTemplateList getCommands() { return commands; }

    public FunctionTemplateList getBuzzCommands() { return buzzCommands; }

    public void setCommands(List<FunctionTemplate> commands) {
        // Iterate in list to classify if buzz or not
        for(FunctionTemplate functionTemplate : commands) {
            addCommand(functionTemplate);
        }
    }

    public void addCommand(FunctionTemplate command) {
        if (command.isBuzzFunction()) {
            buzzCommands.add(command);
        } else {
            commands.add(command);
        }
        setChanged();
        notifyObservers();
    }

    public void clearCommands() {
        this.commands.clear();
        this.buzzCommands.clear();
        setChanged();
        notifyObservers();
    }

    public ProtoMsgStorer getProtoMsgStorer() { return msgLogging; }

    public void registerSendCommand(MessageOuterClass.Message msg) {
        sentCommands.addMsg(msg);
    }

    public ProtoMsgStorer getSentCommandsStorer() { return sentCommands; }
}
