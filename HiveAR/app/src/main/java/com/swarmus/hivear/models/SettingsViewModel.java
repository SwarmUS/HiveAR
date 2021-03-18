package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {
    private MutableLiveData<String> activeDatabaseFolder;

    public MutableLiveData<String> getActiveDatabaseFolder()
    {
        if (activeDatabaseFolder==null) {activeDatabaseFolder = new MutableLiveData<>();}
        return activeDatabaseFolder;
    }
}
