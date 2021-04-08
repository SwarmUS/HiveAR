package com.swarmus.hivear.models;

import android.content.Context;
import android.content.Intent;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.enums.ConnectionStatus;

import java.io.InputStream;

// Interface to be used by SerialDevice/TCPDevice
abstract public class CommunicationDevice {
    static public final String CONNECTION_STATUS_RESULT = "com.swarmus.hivear.CONNECTION_STATUS_RESULT";
    static public final String EXTRA_CONNECTION_STATUS_RESULT = "connectionStatusResult";

    protected Context context;
    protected boolean isActive;
    protected ConnectionCallback connectionCallback;
    protected ConnectionStatus currentStatus;

    protected Object mutex = new Object();

    public CommunicationDevice(Context context, ConnectionCallback connectionCallback) {
        this.context = context;
        this.connectionCallback = connectionCallback;
    }

    public void setActive(boolean active) {this.isActive = active;}
    abstract public void establishConnection();
    abstract public void endConnection();
    abstract public void performConnectionCheck();
    abstract public void sendData(byte[] data);
    abstract public void sendData(String data);
    abstract public void sendData(MessageOuterClass.Message protoMessage);
    abstract public InputStream getDataStream();
    public void broadCastConnectionStatus(ConnectionStatus connectionStatus) {
        if (context != null && isActive) {
            Intent intent = new Intent();
            intent.setAction(CONNECTION_STATUS_RESULT);
            intent.putExtra(EXTRA_CONNECTION_STATUS_RESULT, connectionStatus);
            context.sendBroadcast(intent);
        }
    }

    public interface ConnectionCallback {
        void onConnect();
        void onDisconnect();
        void onConnectError();
    }
}
