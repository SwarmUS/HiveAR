package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.MessageOuterClass;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

public class ProtoMsgViewModel extends ViewModel {
    // First element = most recent
    private final MutableLiveData<LinkedList<String>> msgQueue = new MutableLiveData<>(new LinkedList<>());
    private final int maxCapacity = 10; // We keep last 10 messages
    private final static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public LiveData<LinkedList<String>> getMsgQueue() { return msgQueue; }

    public void addMsg(MessageOuterClass.Message msg) {
        LinkedList<String> msgs = new LinkedList<>(msgQueue.getValue());
        if (msgs.size() == maxCapacity) { msgs.removeLast(); }
        // TODO color text
        String msgString = sdf.format(Calendar.getInstance().getTime());
        msgString += msg.toString();
        msgs.addFirst(msgString);
        msgQueue.setValue(new LinkedList<>(msgs));
    }

    public void clearMsgs() {
        msgQueue.setValue(new LinkedList<>());
    }

    public String getLastMsgs(int nbToRetrieve) {
        LinkedList<String> msgs = msgQueue.getValue();
        nbToRetrieve = Math.min(nbToRetrieve, msgs.size());
        nbToRetrieve = Math.max(nbToRetrieve, 0);
        String logMsg = "";
        for (int i = 0; i < nbToRetrieve; i++) {
            logMsg += "\n==================================\n";
            logMsg += msgs.get(i) + "\n";
        }
        return logMsg;
    }
}
