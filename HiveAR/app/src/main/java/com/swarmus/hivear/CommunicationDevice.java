package com.swarmus.hivear;

import com.felhr.usbserial.UsbSerialInterface;

// Interface to be used by SerialDevice/TCPDevice
public interface CommunicationDevice {
    public void init();
    public void establishConnection();
    public void endConnection();
    public void sendData(byte[] data);
    public byte[] getData();
}
