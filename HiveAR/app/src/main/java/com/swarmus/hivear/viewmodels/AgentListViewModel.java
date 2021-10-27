package com.swarmus.hivear.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.models.Agent;
import com.swarmus.hivear.models.ProtoMsgStorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AgentListViewModel extends ViewModel {
    MutableLiveData<List<Agent>> agentList;
    HashMap<Integer, Integer> apriltagIdConversionMap = new HashMap<>(); // Apriltag id, Agent/board id
    MutableLiveData<ProtoMsgStorer> allAgentsMsgStorerMutableData;
    ProtoMsgStorer allProtoMsgStorer;
    LocalSwarmAgentViewModel localSwarmAgentViewModel;

    public AgentListViewModel() {
        agentList = new MutableLiveData<>(new ArrayList<>());
        allProtoMsgStorer = new ProtoMsgStorer(30, "All", ProtoMsgStorer.NO_AGENT_ID);
        allAgentsMsgStorerMutableData = new MutableLiveData<>(allProtoMsgStorer);
    }

    public void clearAgentList() {
        agentList.setValue(new ArrayList<>());
        allProtoMsgStorer.clear();
        // Keep local msgs
        if (localSwarmAgentViewModel != null &&
                localSwarmAgentViewModel.isLocalSwarmAgentInitialized() &&
                localSwarmAgentViewModel.getProtoMsgStorer() != null) {
            for(MessageOuterClass.Message m : localSwarmAgentViewModel.getProtoMsgStorer().getRawMsgs()) {
                allProtoMsgStorer.addMsg(m);
            }
        }
    }

    public void setLocalSwarmAgentViewModel(LocalSwarmAgentViewModel localSwarmAgentViewModel) {
        this.localSwarmAgentViewModel = localSwarmAgentViewModel;
    }

    public LiveData<List<Agent>> getAgentList() { return agentList; }

    public void addAgent(Agent agent) {
        ArrayList<Agent> agents = new ArrayList<>(agentList.getValue());
        agents.add(agent);
        agentList.setValue(agents);
    }

    // Get agent from apriltag. If conversion was set for this apriltag, get the corresponding agent,
    // else try to get agent from aprilID as it might not have been set in the settings
    public Agent getAgentFromApriltag(int aprilID) {
        if (apriltagIdConversionMap.containsKey(aprilID)) {
            return getAgentFromList(apriltagIdConversionMap.get(aprilID));
        }
        return getAgentFromList(aprilID);
    }

    public Agent getAgentFromList(@NonNull int uid) {
        for (Agent agent : agentList.getValue()) {
            if (agent.getUid() == uid) { return agent; }
        }
        return null;
    }

    public boolean addNewConversion(int boardID, int aprilID) {
        if (!apriltagIdConversionMap.containsKey(aprilID)) {
            apriltagIdConversionMap.put(aprilID, boardID);
            return true;
        }
        return false;
    }

    public void removeConversion(int aprilID) {
        apriltagIdConversionMap.computeIfPresent(aprilID, (k,v) -> null );
    }

    public HashMap<Integer, Integer> getIDConversions() {
        return apriltagIdConversionMap;
    }
    public void storeNewMsg(MessageOuterClass.Message msg) {
        if (msg == null) { return; }

        // Add to all msgs
        allProtoMsgStorer.addMsg(msg);

        // Add to specific agents logging
        int sourceID = msg.getSourceId();
        int destinationID = msg.getDestinationId();

        if (localSwarmAgentViewModel != null && localSwarmAgentViewModel.isLocalSwarmAgentInitialized()) {
            int agentID = localSwarmAgentViewModel.getLocalSwarmAgentID().getValue();
            if (agentID == sourceID || agentID == destinationID) {
                localSwarmAgentViewModel.getProtoMsgStorer().addMsg(msg);
            }
        }

        Agent sourceAgent = getAgentFromList(sourceID);
        if (sourceAgent != null) { sourceAgent.getProtoMsgStorer().addMsg(msg); }

        Agent destinationAgent = getAgentFromList(destinationID);
        if (destinationAgent != null) { destinationAgent.getProtoMsgStorer().addMsg(msg); }
    }

    public void storeSentCommand(MessageOuterClass.Message msg) {
        if (msg == null) { return; }

        // Add to destination agent
        Agent destinationAgent = getAgentFromList(msg.getDestinationId());
        if (destinationAgent != null) { destinationAgent.registerSendCommand(msg); }
    }

    public LiveData<ProtoMsgStorer> getProtoMsgStorer() {
        return allAgentsMsgStorerMutableData;
    }
}
