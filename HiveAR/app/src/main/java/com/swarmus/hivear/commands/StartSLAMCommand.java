package com.swarmus.hivear.commands;

import com.swarmus.hivear.FunctionCall;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.UserCall;

public class StartSLAMCommand extends GenericCommand{

    private final static String START_SLAM_COMMAND ="startSLAM";
    boolean activated;

    public StartSLAMCommand() { this(true);}

    public StartSLAMCommand(boolean activate)
    {
        this.activated = activate;
    }

    @Override
    public MessageOuterClass.Message getCommand() {
        if (message == null)
        {
            FunctionCall.FunctionArgument arg = FunctionCall.FunctionArgument.newBuilder()
                    .setIntArg(activated ? 1 : 0)
                    .build();
            FunctionCall.FunctionCallRequest request = FunctionCall.FunctionCallRequest.newBuilder()
                    .addArguments(arg)
                    .setFunctionName(START_SLAM_COMMAND)
                    .build();
            UserCall.UserCallTarget userCallDestination = UserCall.UserCallTarget.HOST;
            UserCall.UserCallTarget userCallSource = UserCall.UserCallTarget.HOST;
            UserCall.UserCallRequest userCallRequest = UserCall.UserCallRequest.newBuilder()
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
