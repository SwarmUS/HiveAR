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
            messagesString += "DestinationId: " + msg.getDestinationId() + "\n";
            messagesString += "SourceId: " + msg.getSourceId() + "\n";
            switch (msg.getMessageCase()) {
                case REQUEST:
                    messagesString += "Request: " + msg.getRequest() + "\n";
                    switch (msg.getRequest().getMessageCase()) {
                        case USER_CALL:
                            messagesString += "UserCall: " + msg.getRequest().getUserCall() + "\n";
                            switch (msg.getRequest().getUserCall().getRequestCase()) {
                                case FUNCTION_CALL:
                                    messagesString += "Function Call: " + msg.getRequest().getUserCall().getFunctionCall().getFunctionName() + "\n";
                                    break;
                                case FUNCTION_LIST_LENGTH:
                                    messagesString += "Function list length: " + msg.getRequest().getUserCall().getFunctionListLength().toString() + "\n";
                                    break;
                                case FUNCTION_DESCRIPTION:
                                    messagesString += "Function description: " + msg.getRequest().getUserCall().getFunctionDescription().toString() + "\n";
                                    break;
                                case REQUEST_NOT_SET:
                                    messagesString += "Not set: " + msg.getRequest().getUserCall() + "\n";
                                    break;
                            }
                            break;
                        case HIVE_API:
                            messagesString += "HiveAPI: " + msg.getRequest().getHiveApi() + "\n";
                            switch (msg.getRequest().getHiveApi().getRequestCase()) {
                                case ID:
                                    messagesString += "ID: " + msg.getRequest().getHiveApi().getId().toString() + "\n";
                                    break;
                                case REQUEST_NOT_SET:
                                    messagesString += "Not set: " + msg.getRequest().getHiveApi() + "\n";
                                    break;
                            }
                            break;
                        case SWARM_API:
                            messagesString += "SwarmAPI: " + msg.getRequest().getSwarmApi() + "\n";
                            switch (msg.getRequest().getSwarmApi().getRequestCase()) {
                                case ID:
                                    messagesString += "ID: " + msg.getRequest().getSwarmApi().getId().toString() + "\n";
                                    break;
                                case REQUEST_NOT_SET:
                                    messagesString += "Not set: " + msg.getRequest().getSwarmApi() + "\n";
                                    break;
                            }
                            break;
                        case MESSAGE_NOT_SET:
                            messagesString += "Not set: " + msg.getRequest() + "\n";
                            break;
                    }
                    break;
                case RESPONSE:
                    messagesString += "Response: " + msg.getResponse() + "\n";
                    switch (msg.getResponse().getMessageCase()) {
                        case GENERIC:
                            messagesString += "Generic: " + msg.getResponse().getGeneric().getDetails() + "\n";
                            break;
                        case USER_CALL:
                            messagesString += "User call: " + msg.getResponse().getUserCall() + "\n";
                            switch (msg.getResponse().getUserCall().getResponseCase()) {
                                case GENERIC:
                                    messagesString += "Generic: " + msg.getResponse().getUserCall().getGeneric().getDetails() + "\n";
                                    break;
                                case FUNCTION_CALL:
                                    messagesString += "Function Call: " + msg.getResponse().getUserCall().getFunctionCall().getResponse().getDetails() + "\n";
                                    break;
                                case FUNCTION_LIST_LENGTH:
                                    messagesString += "Function list length: " + msg.getResponse().getUserCall().getFunctionListLength().toString() + "\n";
                                    break;
                                case FUNCTION_DESCRIPTION:
                                    messagesString += "Function description: " + msg.getResponse().getUserCall().getFunctionDescription().toString() + "\n";
                                    break;
                                case RESPONSE_NOT_SET:
                                    messagesString += "Not set: " + msg.getResponse().getUserCall() + "\n";
                                    break;
                            }
                            break;
                        case HIVE_API:
                            messagesString += "HiveAPI: " + msg.getResponse().getHiveApi() + "\n";
                            switch (msg.getResponse().getHiveApi().getResponseCase()) {
                                case GENERIC:
                                    messagesString += "Generic: " + msg.getResponse().getHiveApi().getGeneric() + "\n";
                                    break;
                                case ID:
                                    messagesString += "ID: " + msg.getResponse().getHiveApi().getId() + "\n";
                                    break;
                                case RESPONSE_NOT_SET:
                                    messagesString += "Not set: " + msg.getResponse().getHiveApi() + "\n";
                                    break;
                            }
                            break;
                        case SWARM_API:
                            messagesString += "Swarm API: " + msg.getResponse().getSwarmApi() + "\n";
                            switch (msg.getResponse().getSwarmApi().getResponseCase()) {
                                case GENERIC:
                                    messagesString += "Generic: " + msg.getResponse().getSwarmApi().getGeneric() + "\n";
                                    break;
                                case ID:
                                    messagesString += "ID: " + msg.getResponse().getSwarmApi().getId() + "\n";
                                    break;
                                case RESPONSE_NOT_SET:
                                    messagesString += "Not set: " + msg.getResponse().getSwarmApi() + "\n";
                                    break;
                            }
                            break;
                        case MESSAGE_NOT_SET:
                            messagesString += "Not set: " + msg.getResponse() + "\n";
                            break;
                    }
                    break;
                case GREETING:
                    messagesString += "Greeting id: " + msg.getGreeting().getId() + "\n";
                    break;
                case BUZZ:
                    messagesString += "Buzz payload: " + msg.getBuzz().getPayload() + "\n";
                    break;
                case MESSAGE_NOT_SET:
                    messagesString += "Not set: " + msg + "\n";
                    break;
            }
        }
        return messagesString;
    }
}
