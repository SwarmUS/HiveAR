package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class StartSLAMCommand extends GenericCommand{

    private final static String START_SLAM_COMMAND ="startSLAM";
    final boolean activated;

    public StartSLAMCommand() { this(true);}

    public StartSLAMCommand(boolean activate)
    {
        this.activated = activate;
    }

    @Override
    public MessageOuterClass.Message getCommand() {
        if (message == null)
        {
            MessageOuterClass.FunctionArgument arg = MessageOuterClass.FunctionArgument.newBuilder()
                    .setIntArg(activated ? 1 : 0)
                    .build();
            MessageOuterClass.FunctionCallRequest request = MessageOuterClass.FunctionCallRequest.newBuilder()
                    .addArguments(arg)
                    .setFunctionName(START_SLAM_COMMAND)
                    .build();
            MessageOuterClass.UserCallTarget userCallDestination = MessageOuterClass.UserCallTarget.HOST;
            MessageOuterClass.UserCallTarget userCallSource = MessageOuterClass.UserCallTarget.HOST;
            MessageOuterClass.UserCallRequest userCallRequest = MessageOuterClass.UserCallRequest.newBuilder()
                    .setDestination(userCallDestination)
                    .setSource(userCallSource)
                    .setFunctionCall(request)
                    .build();
            MessageOuterClass.Request moveByRequest = MessageOuterClass.Request.newBuilder()
                    .setUserCall(userCallRequest)
                    .build();
            message = MessageOuterClass.Message.newBuilder()
                    .setRequest(moveByRequest)
                    .setDestinationId(1) // TODO temp for now
                    .setSourceId(42) // TODO temp for now
                    .build();
        }
        return message;
    }
}
