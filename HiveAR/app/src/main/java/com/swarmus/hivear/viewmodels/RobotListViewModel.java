package com.swarmus.hivear.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.Robot;

import java.util.List;

public class RobotListViewModel extends ViewModel {
    MutableLiveData<List<Robot>> robotList;

    public MutableLiveData<List<Robot>> getRobotList() {
        if (robotList == null) {robotList = new MutableLiveData<>();}
        return robotList;
    }

    public Robot getRobotFromList(@NonNull int uid) {
        for (Robot robot : robotList.getValue()) {
            if (robot.getUid() == uid) { return robot; }
        }
        return null;
    }
}
