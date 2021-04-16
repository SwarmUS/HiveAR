package com.swarmus.hivear.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.models.ProtoMsgStorer;
import com.swarmus.hivear.models.Robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RobotListViewModel extends ViewModel {
    MutableLiveData<List<Robot>> robotList;
    HashMap<Integer, Integer> apriltagIdConversionMap = new HashMap<>(); // Apriltag id, Robot/board id
    MutableLiveData<ProtoMsgStorer> allRobotsMsgStorerMutableData;
    ProtoMsgStorer protoMsgStorer;
    SwarmAgentInfoViewModel swarmAgentInfoViewModel;

    public RobotListViewModel() {
        robotList = new MutableLiveData<>(new ArrayList<>());
        protoMsgStorer = new ProtoMsgStorer(15, "All");
        allRobotsMsgStorerMutableData = new MutableLiveData<>(protoMsgStorer);
    }

    public void setSwarmAgentInfoViewModel(SwarmAgentInfoViewModel swarmAgentInfoViewModel) {
        this.swarmAgentInfoViewModel = swarmAgentInfoViewModel;
    }

    public LiveData<List<Robot>> getRobotList() {
        if (robotList == null) {robotList = new MutableLiveData<>();}
        return robotList;
    }

    public void addRobot(Robot robot) {
        ArrayList<Robot> robots = new ArrayList<>(robotList.getValue());
        robots.add(robot);
        robotList.setValue(robots);
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
    public void storeNewMsg(MessageOuterClass.Message msg) {
        if (msg == null) { return; }

        // Add to all msgs
        protoMsgStorer.addMsg(msg);

        // Add to specific robots logging
        int sourceID = msg.getSourceId();
        int destinationID = msg.getDestinationId();

        if (swarmAgentInfoViewModel != null) {
            int agentID = swarmAgentInfoViewModel.getSwarmAgentID().getValue();
            if (agentID == (sourceID | destinationID) ) {
                swarmAgentInfoViewModel.getProtoMsgStorer().addMsg(msg);
            }
        }

        Robot sourceRobot = getRobotFromList(sourceID);
        if (sourceRobot != null) { sourceRobot.getProtoMsgStorer().addMsg(msg); }

        Robot destinationRobot = getRobotFromList(destinationID);
        if (destinationRobot != null) { destinationRobot.getProtoMsgStorer().addMsg(msg); }
    }

    public void storeSentCommand(MessageOuterClass.Message msg) {
        if (msg == null) { return; }

        // Add to destination robot
        Robot destinationRobot = getRobotFromList(msg.getDestinationId());
        if (destinationRobot != null) { destinationRobot.registerSendCommand(msg); }
    }

    public LiveData<ProtoMsgStorer> getProtoMsgStorer() {
        return allRobotsMsgStorerMutableData;
    }
}
