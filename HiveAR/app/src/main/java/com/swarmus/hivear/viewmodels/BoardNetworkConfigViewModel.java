package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BoardNetworkConfigViewModel extends ViewModel {
    MutableLiveData<String> networkdSSID;
    MutableLiveData<String> networkPSWD;

    public BoardNetworkConfigViewModel() {
        networkdSSID = new MutableLiveData<>(new String());
        networkPSWD = new MutableLiveData<>(new String());
    }

    public void setNetworkdSSID(String ssid)
    {
        networkdSSID.setValue(ssid);
    }

    public void setNetworkdPSWD(String pswd)
    {
        networkPSWD.setValue(pswd);
    }

    public LiveData<String> getNetworkSSID(){
        return networkdSSID;
    }

    public LiveData<String> getNetworkPSWD(){
        return networkPSWD;
    }
}
