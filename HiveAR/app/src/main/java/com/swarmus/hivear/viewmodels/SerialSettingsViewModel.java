package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

public class SerialSettingsViewModel extends ViewModel {
    private MutableLiveData<HashMap<String, String>> devices;
    private MutableLiveData<String> selectedDevice;

    public SerialSettingsViewModel() {
        devices = new MutableLiveData<>(new HashMap<>());
        selectedDevice = new MutableLiveData<>(new String());
    }

    public LiveData<HashMap<String, String>> getDevices() { return devices; }

    public void setDevices(HashMap<String, String> devices) {
        this.devices.setValue(devices);
    }

    public LiveData<String> getSelectedDevice() { return selectedDevice; }

    public void setSelectedDevice(String selectedDevice) {
        this.selectedDevice.setValue(selectedDevice);
    }
}
