package com.swarmus.hivear.models;

import com.swarmus.hivear.MessageOuterClass;

import java.util.ArrayList;
import java.util.List;

public class FunctionTemplate {

    private List<FunctionTemplateArgument> arguments;
    private String name;

    public FunctionTemplate(String name) {
        this.name = name;
        arguments = new ArrayList<>();
    }

    public void setArguments(List<MessageOuterClass.FunctionDescriptionArgument> protoArgs) {
        arguments = new ArrayList<>();
        for (MessageOuterClass.FunctionDescriptionArgument arg : protoArgs) {
            addArgument(arg);
        }
        this.arguments = arguments;
    }

    public List<FunctionTemplateArgument> getArguments() {
        return arguments;
    }

    public void addArgument(MessageOuterClass.FunctionDescriptionArgument arg) {
        switch (arg.getType()) {
            case INT:
                arguments.add(new FunctionTemplateArgument(arg.getArgumentName(), String.valueOf(0), Integer.class));
                break;
            case FLOAT:
                arguments.add(new FunctionTemplateArgument(arg.getArgumentName(), String.valueOf(0), Float.class));
                break;
            default:
                break;
        }
    }

    public void addArgument(FunctionTemplateArgument arg) {
        arguments.add(arg);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MessageOuterClass.Message getProtoMsg(int swarmAgentID, int swarmAgentDestination) {
        MessageOuterClass.FunctionCallRequest functionCallRequest;
        if (arguments.size() > 0) {
            List<MessageOuterClass.FunctionArgument> functionArguments = new ArrayList<>();
            for (FunctionTemplateArgument argument : arguments) {
                if (argument.getArgumentType().equals(Integer.class)) {
                    functionArguments.add(MessageOuterClass.FunctionArgument.newBuilder().setIntArg((int)argument.getValueFromType()).build());
                } else if (argument.getArgumentType().equals(Float.class)) {
                    functionArguments.add(MessageOuterClass.FunctionArgument.newBuilder().setFloatArg((float)argument.getValueFromType()).build());
                }
            }
            functionCallRequest = MessageOuterClass.FunctionCallRequest.newBuilder()
                    .setFunctionName(name)
                    .addAllArguments(functionArguments)
                    .build();
        } else {
            functionCallRequest = MessageOuterClass.FunctionCallRequest.newBuilder()
                .setFunctionName(name)
                .build();
        }

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
        MessageOuterClass.Message message = MessageOuterClass.Message.newBuilder()
                .setRequest(request)
                .setDestinationId(swarmAgentDestination)
                .setSourceId(swarmAgentID)
                .build();
        return message;
    }
}
