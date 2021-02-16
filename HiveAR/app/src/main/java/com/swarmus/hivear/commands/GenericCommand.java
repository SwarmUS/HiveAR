package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public abstract class GenericCommand {
    protected MessageOuterClass.Message message;
    abstract public MessageOuterClass.Message getCommand();
}
