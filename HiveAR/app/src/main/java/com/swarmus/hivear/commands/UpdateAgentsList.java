package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class UpdateAgentsList extends GenericCommand{
    @Override
    public MessageOuterClass.Message getCommand(int swarmAgentID) {
        if (message == null){
            MessageOuterClass.GetAgentsListRequest getAgentsListRequest =
                    MessageOuterClass.GetAgentsListRequest.newBuilder().build();
            MessageOuterClass.HiveMindHostApiRequest hiveMindHostApiRequest =
                    MessageOuterClass.HiveMindHostApiRequest.newBuilder()
                            .setAgentsList(getAgentsListRequest)
                            .build();
            MessageOuterClass.Request request = MessageOuterClass.Request.newBuilder()
                    .setHivemindHost(hiveMindHostApiRequest)
                    .build();
            message = MessageOuterClass.Message.newBuilder()
                    .setRequest(request)
                    .setDestinationId(swarmAgentID) // TODO find right value
                    .setSourceId(swarmAgentID)
                    .build();
        }
        return message;
    }
}
