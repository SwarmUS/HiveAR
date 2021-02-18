package com.swarmus.hivear.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProtoMsgViewModel extends ViewModel {
    MutableLiveData<String> protoMessages;

    public MutableLiveData<String> getProtoMessages() {
        if (protoMessages==null) {protoMessages = new MutableLiveData<>();}
        return protoMessages;
    }
}
