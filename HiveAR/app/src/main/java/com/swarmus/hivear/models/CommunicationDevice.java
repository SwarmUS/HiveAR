package com.swarmus.hivear.models;

// Interface to be used by SerialDevice/TCPDevice
public interface CommunicationDevice {
    void init();
    void establishConnection();
    void endConnection();
    void sendData(byte[] data);
    void removeReadCallBack();
}
