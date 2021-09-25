package com.swarmus.hivear.commands;

import com.swarmus.hivear.MessageOuterClass;

public class SendNetworkConfigComand extends GenericCommand{
    int destinationID;
    String ssid;
    String pswd;
    boolean isRoot;
    boolean isMesh;

    // Necessary?
    public SendNetworkConfigComand(int destinationID, String ssid, String pswd, boolean isRoot, boolean isMesh) {
        this.destinationID = destinationID;
        this.ssid = ssid;
        this.pswd = pswd;
        this.isRoot = isRoot;
        this.isMesh = isMesh;
    }

    @Override
    public MessageOuterClass.Message getCommand(int swarmAgentID) {
        if (message == null){
            // Missing network config
            MessageOuterClass.HiveConnectNetworkAccess networkAccess =
                    MessageOuterClass.HiveConnectNetworkAccess.newBuilder()
                            .setSsid(ssid)
                            .setPassword(pswd)
                            .build();
            MessageOuterClass.HiveConnectRootNode rootNode =
                    MessageOuterClass.HiveConnectRootNode.newBuilder()
                            .setIsRoot(isRoot)
                            .build();
            MessageOuterClass.HiveConnectMeshEnable meshEnable =
                    MessageOuterClass.HiveConnectMeshEnable.newBuilder()
                            .setUseMesh(isMesh)
                            .build();
            MessageOuterClass.HiveConnectNetworkConfigSetRequest hiveConnectNetworkConfigSetRequest =
                    MessageOuterClass.HiveConnectNetworkConfigSetRequest.newBuilder()
                            .setNetworkAccess(networkAccess)
                            .setRootNode(rootNode)
                            .setMeshEnable(meshEnable)
                            .build();
            MessageOuterClass.HiveConnectHiveMindApi hiveConnectHiveMindApi =
                    MessageOuterClass.HiveConnectHiveMindApi.newBuilder()
                            .setMessageId(swarmAgentID) // Verify if correct value
                            .setNetworkConfigSetRequest(hiveConnectNetworkConfigSetRequest)
                            .build();
            message = MessageOuterClass.Message.newBuilder()
                    .setHiveconnectHivemind(hiveConnectHiveMindApi)
                    .setDestinationId(destinationID)
                    .setSourceId(swarmAgentID)
                    .build();
        }
        return message;
    }
}
