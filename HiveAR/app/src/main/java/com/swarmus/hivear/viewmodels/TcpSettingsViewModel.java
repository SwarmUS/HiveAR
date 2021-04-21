package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TcpSettingsViewModel extends ViewModel {
    private MutableLiveData<String> ipAddress;
    private MutableLiveData<Integer> port;

    public TcpSettingsViewModel() {
        ipAddress = new MutableLiveData<>(new String());
        port = new MutableLiveData<>();
    }

    public LiveData<String> getIpAddress() { return ipAddress; }

    public void setIpAddress(String ipAddress) {
        this.ipAddress.setValue(ipAddress);
    }

    public LiveData<Integer> getPort() { return port; }

    public void setPort(int port) {
        this.port.setValue(port);
    }

}
