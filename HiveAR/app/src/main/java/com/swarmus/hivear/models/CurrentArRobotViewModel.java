package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CurrentArRobotViewModel extends ViewModel {
    MutableLiveData<Robot> selectedRobot;

    public MutableLiveData<Robot> getSelectedRobot() {
        if (selectedRobot == null) {selectedRobot = new MutableLiveData<>(); }
        return selectedRobot;
    }
}