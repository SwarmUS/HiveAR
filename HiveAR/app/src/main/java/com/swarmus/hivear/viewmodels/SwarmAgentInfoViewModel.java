package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SwarmAgentInfoViewModel extends CommandListVM {
    private MutableLiveData<Integer> swarmAgentID;

    public static final int DEFAULT_SWARM_AGENT_ID = -1;
    public static final int BROADCAST_AGENT_ID = 0;

    public SwarmAgentInfoViewModel(){this.listTitle = "Local Buzz";}

    public LiveData<Integer> getSwarmAgentID() {
        if (swarmAgentID == null) {swarmAgentID = new MutableLiveData<>(42);} // TODO TEMP FOR TESTING
        return swarmAgentID;
    }

    public void setSwarmAgentID(int id) {
        swarmAgentID = new MutableLiveData<>(id);
    }

    public boolean isAgentInitialized() {return getSwarmAgentID().getValue() != DEFAULT_SWARM_AGENT_ID;}
}
