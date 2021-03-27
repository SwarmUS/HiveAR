package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

// TODO example, probably not done like that
public class UpdateSwarmContentCommand extends GenericCommand{
    private static final String UPDATE_SWARM_CONTENT_NAME = "updateSwarmContent";
    @Override
    public MessageOuterClass.Message getCommand() {
        if (message == null) {
            MessageOuterClass.FunctionCallRequest functionCallRequest = MessageOuterClass.FunctionCallRequest.newBuilder()
                    .setFunctionName(UPDATE_SWARM_CONTENT_NAME)
                    .build();
            MessageOuterClass.UserCallTarget userCallDestination = MessageOuterClass.UserCallTarget.BUZZ;
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
                    .setDestinationId(1) // TODO temp for now -> ask what should it be
                    .setSourceId(42) // TODO temp for now
                    .build();
        }
        return message;
    }
}
