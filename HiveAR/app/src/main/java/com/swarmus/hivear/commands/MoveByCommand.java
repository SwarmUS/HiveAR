package com.swarmus.hivear.commands;

import com.swarmus.hivear.FunctionCall;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.UserCall;

public class MoveByCommand extends GenericCommand{

    private final static String MOVE_BY_FUNCTION_NAME ="moveBy";
    float x,y;

    public MoveByCommand(float x, float y)
    {
        this.x = x;
        this.y = y;
        this.message = MessageOuterClass.Message.getDefaultInstance();
    }

    @Override
    public MessageOuterClass.Message getCommand() {
        if (!message.hasRequest())
        {
            FunctionCall.FunctionArgument argX = FunctionCall.FunctionArgument.newBuilder()
                    .setFloatArg(x)
                    .build();
            FunctionCall.FunctionArgument argY = FunctionCall.FunctionArgument.newBuilder()
                    .setFloatArg(y)
                    .build();
            FunctionCall.FunctionCallRequest request = FunctionCall.FunctionCallRequest.newBuilder()
                    .addArguments(argX)
                    .addArguments(argY)
                    .setFunctionName(MOVE_BY_FUNCTION_NAME)
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
