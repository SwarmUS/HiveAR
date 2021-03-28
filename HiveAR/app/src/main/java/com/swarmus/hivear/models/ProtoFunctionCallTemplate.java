package com.swarmus.hivear.models;

import com.swarmus.hivear.MessageOuterClass;

import java.util.ArrayList;
import java.util.List;

public class ProtoFunctionCallTemplate {

    public class Argument<T> {
        private String name;
        private T value;

        public Argument(String name, T value) {
            this.name = name;
            this.value = value;
        }

        public T getValue() {return value;}
        public String getName() {return name;}
    }

    private List<Argument> arguments;
    private String name;

    public ProtoFunctionCallTemplate(String name) {
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

    public List<Argument> getArguments() {
        return arguments;
    }

    public void addArgument(MessageOuterClass.FunctionDescriptionArgument arg) {
        switch (arg.getType()) {
            case INT:
                arguments.add(new Argument(arg.getArgumentName(), new Integer(0)));
                break;
            case FLOAT:
                arguments.add(new Argument(arg.getArgumentName(), new Float(0)));
                break;
            default:
                break;
        }
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
            for (Argument argument : arguments) {
                if (argument.getValue() instanceof Integer) {
                    functionArguments.add(MessageOuterClass.FunctionArgument.newBuilder().setIntArg((int)argument.getValue()).build());
                } else if (argument.getValue() instanceof Float) {
                    functionArguments.add(MessageOuterClass.FunctionArgument.newBuilder().setFloatArg((float)argument.getValue()).build());
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
