package com.swarmus.hivear.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.Robot;

import java.util.HashMap;
import java.util.List;

public class RobotListViewModel extends ViewModel {
    MutableLiveData<List<Robot>> robotList;
    HashMap<Integer, Integer> apriltagIdConversionMap = new HashMap<>(); // Apriltag id, Robot/board id

    public MutableLiveData<List<Robot>> getRobotList() {
        if (robotList == null) {robotList = new MutableLiveData<>();}
        return robotList;
    }

    // Get robot from apriltag. If conversion was set for this apriltag, get the corresponding robot,
    // else try to get robot from aprilID as it might not have been set in the settings
    public Robot getRobotFromApriltag(int aprilID) {
        if (apriltagIdConversionMap.containsKey(aprilID)) {
            return getRobotFromList(apriltagIdConversionMap.get(aprilID));
        }
        return getRobotFromList(aprilID);
    }

    public Robot getRobotFromList(@NonNull int uid) {
        for (Robot robot : robotList.getValue()) {
            if (robot.getUid() == uid) { return robot; }
        }
        return null;
    }

    public boolean addNewConversion(int boardID, int aprilID) {
        if (!apriltagIdConversionMap.containsKey(aprilID)) {
            apriltagIdConversionMap.put(aprilID, boardID);
            return true;
        }
        return false;
    }

    public void removeConversion(int aprilID) {
        apriltagIdConversionMap.computeIfPresent(aprilID, (k,v) -> null );
    }

    public HashMap<Integer, Integer> getIDConversions() {
        return apriltagIdConversionMap;
    }
}
