package com.swarmus.hivear.models;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.viewmodels.ProtoMsgViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Observable;

public class ProtoMsgStorer extends Observable {

    public static final int NO_AGENT_ID = -1;
    private final LinkedList<MessageOuterClass.Message> msgQueue;
    private final int maxCapacity;
    private final String uniqueName; // Used to identify the current ProtoMsgStorer to show
    private int agentID;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public ProtoMsgStorer(int maxCapacity, String uniqueName, int agentID) {
        this.maxCapacity = maxCapacity;
        msgQueue = new LinkedList<>();
        this.uniqueName = uniqueName;
        this.agentID = agentID;
    }

    public int getAgentID() { return agentID; }

    public void setAgentID(int id) {agentID = id;}

    // We add element as first of queue to be the first to show. (More useful for logging to see latest received)
    public void addMsg(MessageOuterClass.Message msg) {
        if (msgQueue.size() == maxCapacity) { msgQueue.removeLast(); }
        msgQueue.addFirst(msg);
        setChanged();
        notifyObservers();
    }

    public LinkedList<MessageOuterClass.Message> getRawMsgs() {
        return msgQueue == null ? new LinkedList<>() : msgQueue;
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

    public String getLoggingStringShort(int nbToRetrieve) {
        String loggingString = "";

        nbToRetrieve = Math.min(nbToRetrieve, msgQueue.size());
        nbToRetrieve = Math.max(nbToRetrieve, 0);
        for (int i = 0; i < nbToRetrieve; i++) {
            MessageOuterClass.Message m = msgQueue.get(i);
            int source = m.getSourceId();
            int  destination  = m.getDestinationId();
            String type = "";
            switch (m.getMessageCase()) {
                case REQUEST:
                    type = ProtoMsgViewModel.requestPattern;
                    break;
                case RESPONSE:
                    type = ProtoMsgViewModel.responsePattern;
                    break;
                case GREETING:
                    type = ProtoMsgViewModel.greetingPattern;
                    break;
                case VM:
                    type = ProtoMsgViewModel.vmPattern;
                    break;
                case NETWORK:
                    type = ProtoMsgViewModel.networkPattern;
                    break;
                case INTERLOC:
                    type = ProtoMsgViewModel.interlocPattern;
                    break;
                case HIVECONNECT_HIVEMIND:
                    type = ProtoMsgViewModel.hiveconnectHivemindPattern;
                    break;
                case MESSAGE_NOT_SET:
                    type = ProtoMsgViewModel.nullPattern;
                    break;
            }

            loggingString += String.format("%s: from %d to %d.", type, source, destination);
            if (i != nbToRetrieve - 1) {
                loggingString += "\n";
            }
        }
        return loggingString;
    }

    public String getSimplifiedLoggingString(int nbToRetrieve) {
        String loggingString = "";
        nbToRetrieve = Math.min(nbToRetrieve, msgQueue.size());
        nbToRetrieve = Math.max(nbToRetrieve, 0);

        // Create a string with functioncall with syntax:
        // <Function1Name>(<arg1>, <arg2>, ..., <argN>)
        // <Function2Name>(<arg1>, <arg2>, ..., <argN>)
        // <Function3Name>(<arg1>, <arg2>, ..., <argN>)
        for (int i = 0; i < nbToRetrieve; i++) {
            MessageOuterClass.Message msg = msgQueue.get(i);
            if (msg.hasRequest() && msg.getRequest().hasUserCall() && msg.getRequest().getUserCall().hasFunctionCall()) {
                MessageOuterClass.FunctionCallRequest functionCallRequest = msg.getRequest().getUserCall().getFunctionCall();
                loggingString += functionCallRequest.getFunctionName();
                loggingString += "(";
                for (MessageOuterClass.FunctionArgument argument : functionCallRequest.getArgumentsList()) {
                    switch (argument.getArgumentCase()) {
                        case INT_ARG:
                            loggingString += Long.toString(argument.getIntArg());
                            break;
                        case FLOAT_ARG:
                            loggingString += Float.toString(argument.getFloatArg());
                            break;
                        case ARGUMENT_NOT_SET:
                            continue;
                    }
                    if (functionCallRequest.getArgumentsList().indexOf(argument) != functionCallRequest.getArgumentsList().size() - 1) {
                        loggingString += ", ";
                    }
                }
                loggingString += ")";
            }
            if (i != nbToRetrieve - 1) {
                loggingString += "\n";
            }
        }

        return loggingString;
    }

    public void clear() {
        msgQueue.clear();
        setChanged();
        notifyObservers();
    }

    public String getUniqueName() {return uniqueName;}
}
