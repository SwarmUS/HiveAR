package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swarmus.hivear.models.ProtoMsgStorer;

public class SwarmAgentInfoViewModel extends CommandListVM {
    private static final int STORER_MAX_CAPACITY = 5;
    private MutableLiveData<Integer> swarmAgentID;
    private ProtoMsgStorer protoMsgStorer;

    public static final int DEFAULT_SWARM_AGENT_ID = -1;
    public static final int BROADCAST_AGENT_ID = 0;

    public SwarmAgentInfoViewModel(){
        this.listTitle = "Local Buzz";
        swarmAgentID = new MutableLiveData<>(DEFAULT_SWARM_AGENT_ID);
        protoMsgStorer = new ProtoMsgStorer(STORER_MAX_CAPACITY, "Local", DEFAULT_SWARM_AGENT_ID);
    }

    public LiveData<Integer> getSwarmAgentID() { return swarmAgentID; }

    public void setSwarmAgentID(int id) {
        swarmAgentID.setValue(id);
        // Erase last storer on new id associated to local agent
        protoMsgStorer = new ProtoMsgStorer(STORER_MAX_CAPACITY, "Local", id);
    }

    public boolean isAgentInitialized() {return getSwarmAgentID().getValue() != DEFAULT_SWARM_AGENT_ID;}

    public ProtoMsgStorer getProtoMsgStorer() { return protoMsgStorer; }
}
