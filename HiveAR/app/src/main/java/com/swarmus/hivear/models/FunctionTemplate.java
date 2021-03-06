package com.swarmus.hivear.models;

import com.swarmus.hivear.MessageOuterClass;

import java.util.ArrayList;
import java.util.List;

public class FunctionTemplate {

    private List<FunctionTemplateArgument> arguments;
    private String name;
    private boolean isBuzzFunction;

    public FunctionTemplate(String name, boolean isBuzz) {
        this.name = name;
        arguments = new ArrayList<>();
        this.isBuzzFunction = isBuzz;
    }

    // Deep copy
    public FunctionTemplate(FunctionTemplate f) {
        this.arguments = new ArrayList<>();
        for (FunctionTemplateArgument argument : f.arguments) {
            this.arguments.add(new FunctionTemplateArgument(argument));
        }
        this.name = f.name;
        this.isBuzzFunction = f.isBuzzFunction;
    }

    public void setArguments(List<MessageOuterClass.FunctionDescriptionArgument> protoArgs) {
        arguments = new ArrayList<>();
        for (MessageOuterClass.FunctionDescriptionArgument arg : protoArgs) {
            addArgument(arg);
        }
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
                    functionArguments.add(MessageOuterClass.FunctionArgument.newBuilder().setIntArg((Integer)argument.getValueFromType()).build());
                } else if (argument.getArgumentType().equals(Float.class)) {
                    functionArguments.add(MessageOuterClass.FunctionArgument.newBuilder().setFloatArg((Float)argument.getValueFromType()).build());
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

        MessageOuterClass.UserCallTarget userCallDestination = isBuzzFunction ?
                MessageOuterClass.UserCallTarget.BUZZ : MessageOuterClass.UserCallTarget.HOST;
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

    public boolean isBuzzFunction() {
        return isBuzzFunction;
    }

    public boolean equals(Object other) {
        if (other instanceof FunctionTemplate) {
            FunctionTemplate otherF = (FunctionTemplate)other;
            if (!this.name.equals(otherF.name) ||
                    this.isBuzzFunction != otherF.isBuzzFunction ||
                    this.arguments.size() != otherF.arguments.size()) {
                return false;
            }

            for(FunctionTemplateArgument a : arguments) {
                if (!otherF.arguments.contains(a)) {
                    return false;
                }
            }

            // All exceptions have passed, consider as same method
            return true;
        }

        return false;

    }


}
