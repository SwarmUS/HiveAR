package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.FunctionTemplate;

import java.util.List;

public class BroadcastInfoViewModel extends ViewModel {
    private MutableLiveData<List<FunctionTemplate>> swarmCommandList;

    public MutableLiveData<List<FunctionTemplate>> getSwarmCommandList() {
        if (swarmCommandList == null) { swarmCommandList = new MutableLiveData<>(); }
        return swarmCommandList;
    }
}
