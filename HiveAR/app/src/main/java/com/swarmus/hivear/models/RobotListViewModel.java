package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class RobotListViewModel extends ViewModel {
    MutableLiveData<List<Robot>> robotList;

    public MutableLiveData<List<Robot>> getRobotList() {
        if (robotList == null) {robotList = new MutableLiveData<>();}
        return robotList;
    }
}
