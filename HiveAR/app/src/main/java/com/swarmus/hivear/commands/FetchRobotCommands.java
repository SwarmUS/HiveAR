package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class FetchRobotCommands extends GenericCommand{
    private final static String FETCH_COMMAND_NAME = "fetchCommands";
    int uid;

    public FetchRobotCommands(int uid) {
        this.uid = uid;
    }

    @Override
    public MessageOuterClass.Message getCommand() {
        if (message == null){
            MessageOuterClass.FunctionCallRequest functionCallRequest = MessageOuterClass.FunctionCallRequest.newBuilder()
                    .setFunctionName(FETCH_COMMAND_NAME)
                    .build();
            MessageOuterClass.UserCallTarget userCallDestination = MessageOuterClass.UserCallTarget.HOST;
            MessageOuterClass.UserCallTarget userCallSource = MessageOuterClass.UserCallTarget.HOST;
            MessageOuterClass.UserCallRequest userCallRequest = MessageOuterClass.UserCallRequest.newBuilder()
                    .setDestination(userCallDestination)
                    .setSource(userCallSource)
                    .setFunctionCall(functionCallRequest)
                    .build();
            MessageOuterClass.Request request = MessageOuterClass.Request.newBuilder()
                    .setUserCall(userCallRequest)
                    .build();
            message = MessageOuterClass.Message.newBuilder()
                    .setRequest(request)
                    .setDestinationId(uid)
                    .setSourceId(42) // TODO temp for now
                    .build();
        }
        return message;
    }
}
