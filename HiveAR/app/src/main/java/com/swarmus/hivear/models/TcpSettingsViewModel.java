package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TcpSettingsViewModel extends ViewModel {
    private MutableLiveData<String> ipAddress;
    private MutableLiveData<Integer> port;

    public MutableLiveData<String> getIpAddress() {
        if (ipAddress == null) {
            ipAddress = new MutableLiveData<>();
        }
        return this.ipAddress;
    }

    public MutableLiveData<Integer> getPort() {
        if (port == null) {
            port = new MutableLiveData<>();
        }
        return this.port;
    }

}
