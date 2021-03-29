package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class FetchRobotCommands extends GenericCommand{
    int uid;

    public FetchRobotCommands(int uid) {
        this.uid = uid;
    }

    @Override
    public MessageOuterClass.Message getCommand(int swarmAgentID) {
        if (message == null){
            MessageOuterClass.FunctionListLengthRequest functionListLengthRequest =
                    MessageOuterClass.FunctionListLengthRequest.newBuilder().build();
            MessageOuterClass.UserCallTarget userCallDestination = MessageOuterClass.UserCallTarget.HOST;
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
