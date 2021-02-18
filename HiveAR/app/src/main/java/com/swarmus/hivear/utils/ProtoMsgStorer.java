package com.swarmus.hivear.utils;

import androidx.annotation.NonNull;

import com.swarmus.hivear.MessageOuterClass;

import java.util.LinkedList;
import java.util.Queue;

public class ProtoMsgStorer {
    private Queue<MessageOuterClass.Message> msgQueue;
    int maxCapacity = 0;

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
            messagesString += "DestinationId: " + msg.getDestinationId() + "\n";
            messagesString += "SourceId: " + msg.getSourceId() + "\n";
            if (msg.hasRequest()) { messagesString += "Request: " + msg.getRequest() + "\n";}
            else { messagesString += "Response: " + msg.getResponse() + "\n";}
        }
        return messagesString;
    }
}
