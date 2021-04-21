package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swarmus.hivear.models.ProtoMsgStorer;

public class LocalSwarmAgentViewModel extends CommandListVM {
    private MutableLiveData<Integer> swarmAgentID;
    private ProtoMsgStorer protoMsgStorer = new ProtoMsgStorer(5, "Local");

    public static final int DEFAULT_SWARM_AGENT_ID = -1;
    public static final int BROADCAST_AGENT_ID = 0;

    public LocalSwarmAgentViewModel(){
        this.listTitle = "Local Buzz";
        swarmAgentID = new MutableLiveData<>(DEFAULT_SWARM_AGENT_ID);
    }

    public LiveData<Integer> getLocalSwarmAgentID() { return swarmAgentID; }

    public void setLocalSwarmAgentID(int id) { swarmAgentID.setValue(id); }

    public boolean isLocalSwarmAgentInitialized() {return getLocalSwarmAgentID().getValue() != DEFAULT_SWARM_AGENT_ID;}

    public ProtoMsgStorer getProtoMsgStorer() { return protoMsgStorer; }
}
