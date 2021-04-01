package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class FetchRobotCommands extends GenericCommand{
    int uid;
    boolean isBuzz;

    public FetchRobotCommands(int uid, boolean isBuzz) {
        this.uid = uid;
        this.isBuzz = isBuzz;
    }

    @Override
    public MessageOuterClass.Message getCommand(int swarmAgentID) {
        if (message == null){
            MessageOuterClass.FunctionListLengthRequest functionListLengthRequest =
                    MessageOuterClass.FunctionListLengthRequest.newBuilder().build();
            MessageOuterClass.UserCallTarget userCallDestination = isBuzz ?
                    MessageOuterClass.UserCallTarget.BUZZ : MessageOuterClass.UserCallTarget.HOST;
            MessageOuterClass.UserCallTarget userCallSource = MessageOuterClass.UserCallTarget.HOST;
            MessageOuterClass.UserCallRequest userCallRequest = MessageOuterClass.UserCallRequest.newBuilder()
                    .setDestination(userCallDestination)
                    .setSource(userCallSource)
                    .setFunctionListLength(functionListLengthRequest)
                    .build();
            MessageOuterClass.Request request = MessageOuterClass.Request.newBuilder()
                    .setUserCall(userCallRequest)
                    .build();
            message = MessageOuterClass.Message.newBuilder()
                    .setRequest(request)
                    .setDestinationId(uid)
                    .setSourceId(swarmAgentID)
                    .build();
        }
        return message;
    }
}
