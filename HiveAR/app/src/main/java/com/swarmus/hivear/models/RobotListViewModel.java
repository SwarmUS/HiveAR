package com.swarmus.hivear.models;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class RobotListViewModel extends ViewModel {
    MutableLiveData<List<Robot>> robotList;

    public MutableLiveData<List<Robot>> getRobotList() {
        if (robotList == null) {robotList = new MutableLiveData<>();}
        return robotList;
    }

    public Robot getRobotFromList(@NonNull String name,@NonNull int uid) {
        for (Robot robot : robotList.getValue()) {
            if (name.equals(robot.getName()) && robot.getUid() == uid) { return robot; }
        }
        return null;
    }
}
