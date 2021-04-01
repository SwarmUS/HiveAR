package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SwarmAgentInfoViewModel extends ViewModel {
    private MutableLiveData<Integer> swarmAgentID;
    public static final int DEFAULT_SWARM_AGENT_ID = -1;
    public static final int BROADCAST_AGENT_ID = 0;

    public MutableLiveData<Integer> getSwarmAgentID() {
        if (swarmAgentID == null) {swarmAgentID = new MutableLiveData<>(DEFAULT_SWARM_AGENT_ID);}
        return swarmAgentID;
    }

    public boolean isAgentInitialized() {return getSwarmAgentID().getValue() != DEFAULT_SWARM_AGENT_ID;}
}
