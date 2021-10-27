package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swarmus.hivear.models.ProtoMsgStorer;

public class LocalSwarmAgentViewModel extends CommandListVM {
    private static final int STORER_MAX_CAPACITY = 15;
    private static final String LOCAL_STORER_NAME = "Local";
    private MutableLiveData<Integer> swarmAgentID;
    private ProtoMsgStorer protoMsgStorer;

    public static final int DEFAULT_SWARM_AGENT_ID = -1;
    public static final int BROADCAST_AGENT_ID = 0;

    public LocalSwarmAgentViewModel(){
        this.listTitle = "Local Buzz";
        swarmAgentID = new MutableLiveData<>(DEFAULT_SWARM_AGENT_ID);
        protoMsgStorer = new ProtoMsgStorer(STORER_MAX_CAPACITY, LOCAL_STORER_NAME, DEFAULT_SWARM_AGENT_ID);
    }

    public LiveData<Integer> getLocalSwarmAgentID() { return swarmAgentID; }

    public void setLocalSwarmAgentID(int id, boolean fromMainThread) {
        if (fromMainThread) {
            swarmAgentID.setValue(id);
        }
        else {
            swarmAgentID.postValue(id);
        }
        if (protoMsgStorer != null) {
            protoMsgStorer.clear();
            protoMsgStorer.setAgentID(id);
        } else {
            protoMsgStorer = new ProtoMsgStorer(STORER_MAX_CAPACITY, LOCAL_STORER_NAME, id);
        }
    }

    public boolean isLocalSwarmAgentInitialized() {return getLocalSwarmAgentID().getValue() != DEFAULT_SWARM_AGENT_ID;}

    public ProtoMsgStorer getProtoMsgStorer() { return protoMsgStorer; }
}
