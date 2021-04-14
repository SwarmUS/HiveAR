package com.swarmus.hivear.models;

import com.swarmus.hivear.MessageOuterClass;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Observable;

public class ProtoMsgStorer extends Observable {

    private final LinkedList<MessageOuterClass.Message> msgQueue;
    private final int maxCapacity;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public ProtoMsgStorer(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        msgQueue = new LinkedList<>();
    }

    // We add element as first of queue to be the first to show. (More useful for logging to see latest received)
    public void addMsg(MessageOuterClass.Message msg) {
        if (msgQueue.size() == maxCapacity) { msgQueue.removeLast(); }
        msgQueue.addFirst(msg);
        setChanged();
        notifyObservers();
    }

    public String getLoggingString(int nbToRetrieve) {
        String loggingString = "";

        nbToRetrieve = Math.min(nbToRetrieve, msgQueue.size());
        nbToRetrieve = Math.max(nbToRetrieve, 0);
        for (int i = 0; i < nbToRetrieve; i++) {
            loggingString += "\n==================================\n";
            loggingString += "Received at: " + sdf.format(Calendar.getInstance().getTime()) + "\n";
            loggingString += msgQueue.get(i).toString() + "\n";
        }

        return loggingString;
    }

    public void clear() {
        msgQueue.clear();
        setChanged();
        notifyObservers();
    }

}
