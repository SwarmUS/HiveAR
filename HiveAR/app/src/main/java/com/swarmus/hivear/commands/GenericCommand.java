package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public abstract class GenericCommand {
    protected MessageOuterClass.Message moveByMessage;
    abstract public MessageOuterClass.Message getCommand();
}
