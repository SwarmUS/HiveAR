package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

public class SerialSettingsViewModel extends ViewModel {
    private MutableLiveData<HashMap<String, String>> devices;
    private MutableLiveData<String> selectedDevices;

    public MutableLiveData<HashMap<String, String>> getDevices()
    {
        if (devices==null) {devices = new MutableLiveData<>();}
        return devices;
    }

    public MutableLiveData<String> getSelectedDevice()
    {
        if (selectedDevices==null) {selectedDevices = new MutableLiveData<>();}
        return selectedDevices;
    }
}
