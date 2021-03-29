package com.swarmus.hivear.utils;

import androidx.annotation.NonNull;

import com.swarmus.hivear.MessageOuterClass;

import java.util.LinkedList;
import java.util.Queue;

public class ProtoMsgStorer {
    private final Queue<MessageOuterClass.Message> msgQueue;
    int maxCapacity;

    public ProtoMsgStorer(int size) {
        maxCapacity = size;
        msgQueue = new LinkedList<>();
    }

    public void addMsg(MessageOuterClass.Message msg) {
        if (msgQueue.size() == maxCapacity) { msgQueue.poll(); }
        msgQueue.add(msg);
    }

    public boolean isEmpty() {
        return msgQueue.isEmpty();
    }

    @NonNull
    @Override
    public String toString() {
        String messagesString = "";
        for (MessageOuterClass.Message msg : msgQueue) {
            messagesString += "\nProto Message:\n";
            messagesString += msg + "\n";
        }
        return messagesString;
    }
}
