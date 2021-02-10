package com.swarmus.hivear.models;

import android.content.Context;
import android.content.Intent;

import com.swarmus.hivear.enums.ConnectionStatus;

import java.io.InputStream;

// Interface to be used by SerialDevice/TCPDevice
abstract public class CommunicationDevice {
    static public final String CONNECTION_STATUS_RESULT = "com.swarmus.hivear.CONNECTION_STATUS_RESULT";
    static public final String EXTRA_CONNECTION_STATUS_RESULT = "connectionStatusResult";

    protected Context context;

    public void init(Context context){this.context=context;}
    abstract public void establishConnection();
    abstract public void endConnection();
    abstract public void sendData(byte[] data);
    abstract public void sendData(String data);
    abstract public InputStream getDataStream();
    abstract public void removeReadCallBack();
    public void broadCastConnectionStatus(ConnectionStatus connectionStatus) {
        if (context != null) {
            Intent intent = new Intent();
            intent.setAction(CONNECTION_STATUS_RESULT);
            intent.putExtra(EXTRA_CONNECTION_STATUS_RESULT, connectionStatus);
            context.sendBroadcast(intent);
        }
    }
}
