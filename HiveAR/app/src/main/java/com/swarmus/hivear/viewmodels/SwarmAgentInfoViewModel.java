package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.FunctionTemplate;

import java.util.List;

public class SwarmAgentInfoViewModel extends ViewModel {
    private MutableLiveData<Integer> swarmAgentID;
    MutableLiveData<List<FunctionTemplate>> commands;

    public static final int DEFAULT_SWARM_AGENT_ID = -1;
    public static final int BROADCAST_AGENT_ID = 0;

    public MutableLiveData<Integer> getSwarmAgentID() {
        if (swarmAgentID == null) {swarmAgentID = new MutableLiveData<>(DEFAULT_SWARM_AGENT_ID);}
        return swarmAgentID;
    }

    public boolean isAgentInitialized() {return getSwarmAgentID().getValue() != DEFAULT_SWARM_AGENT_ID;}

    public MutableLiveData<List<FunctionTemplate>> getCommands() {
        if (commands == null) { commands = new MutableLiveData<>(); }
        return commands;
    }
}
