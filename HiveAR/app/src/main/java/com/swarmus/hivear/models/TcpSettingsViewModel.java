package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.enums.ConnectionStatus;

public class TcpSettingsViewModel extends ViewModel {
    private MutableLiveData<String> ipAddress;
    private MutableLiveData<Integer> port;
    private MutableLiveData<ConnectionStatus> connectionStatus;

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

    public MutableLiveData<ConnectionStatus> getConnectionStatus() {
        if (connectionStatus == null) {
            connectionStatus = new MutableLiveData<>();
        }
        return this.connectionStatus;
    }

}
