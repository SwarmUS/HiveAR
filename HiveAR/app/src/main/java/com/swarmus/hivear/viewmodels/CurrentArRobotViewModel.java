package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.Robot;

public class CurrentArRobotViewModel extends ViewModel {
    MutableLiveData<Robot> selectedRobot;

    public MutableLiveData<Robot> getSelectedRobot() {
        if (selectedRobot == null) {selectedRobot = new MutableLiveData<>(); }
        return selectedRobot;
    }
}