package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class MoveByCommand extends GenericCommand{

    private final static String MOVE_BY_FUNCTION_NAME ="moveBy";
    final float x;
    final float y;

    public MoveByCommand() { this(0, 0);}

    public MoveByCommand(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public MessageOuterClass.Message getCommand(int swarmAgentID) {
        if (message == null)
        {
            MessageOuterClass.FunctionArgument argX = MessageOuterClass.FunctionArgument.newBuilder()
                    .setFloatArg(x)
                    .build();
            MessageOuterClass.FunctionArgument argY = MessageOuterClass.FunctionArgument.newBuilder()
                    .setFloatArg(y)
                    .build();
            MessageOuterClass.FunctionCallRequest request = MessageOuterClass.FunctionCallRequest.newBuilder()
                    .addArguments(argX)
                    .addArguments(argY)
                    .setFunctionName(MOVE_BY_FUNCTION_NAME)
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
                    .setSourceId(swarmAgentID)
                    .build();
        }
        return message;
    }
}
