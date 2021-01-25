package com.swarmus.hivear;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Iterator;

public class SerialDevice implements CommunicationDevice {
    private UsbSerialInterface.UsbReadCallback readCB;
    private Context context;
    private PendingIntent permissionIntent;
    private UsbDevice device;
    private UsbManager manager;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serial;
    private SerialDevice.UsbReceiver usbReceiver;

    public static final String ACTION_SERIAL_DEVICE_CHANGED = "com.swarmus.hivear.SERIAL_DEVICE_CHANGED";
    public static final String EXTRA_SERIAL_DEVICE_CHANGED = "deviceName";
    private static final String ACTION_USE_PERMISSION = "com.swarmus.hivear.USB_PERMISSION";

    public SerialDevice(Context context) { this.context = context; }

    @Override
    public void init() {
        if (context != null)
        {
            usbReceiver = new SerialDevice.UsbReceiver();
            IntentFilter usbDeviceFilter = new IntentFilter();
            usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            context.getApplicationContext().registerReceiver(usbReceiver, usbDeviceFilter);

            // Load devices
            manager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
            permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                    ACTION_USE_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USE_PERMISSION);
            context.getApplicationContext().registerReceiver(usbReceiverPermission, filter);
        }
    }

    @Override
    public void establishConnection() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        String i = "";
        while(deviceIterator.hasNext()) {
            device = deviceIterator.next();
            manager.requestPermission(device, permissionIntent);
            i += "\n" + "DeviceID: " + device.getDeviceId() + "\n"
                    + "DeviceName: " + device.getDeviceName() + "\n"
                    + "DeviceClass: " + device.getDeviceClass() + " - "
                    + "DeviceSubClass: " + device.getDeviceSubclass() + "\n"
                    + "VendorID: " + device.getVendorId() + "\n"
                    + "ProductID: " + device.getProductId() + "\n"
                    + "Protocol: " + device.getDeviceProtocol() + "\n";
        }
        Log.d("INFO", "I: " + i);
        if (device != null) {
            changeDeviceName(device.getProductName());
            listenToSerial();
        }
    }

    @Override
    public void endConnection() {
        changeDeviceName(null);
        stopListenToSerial();
    }

    @Override
    public void sendData(byte[] data) {
        if (serial != null)
            serial.write(data);
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    public void setReadCB(UsbSerialInterface.UsbReadCallback readCB) {this.readCB = readCB;}

    @Nullable
    private void changeDeviceName(@Nullable String deviceName) {
        Intent intent = new Intent();
        intent.setAction(ACTION_SERIAL_DEVICE_CHANGED);
        intent.putExtra(EXTRA_SERIAL_DEVICE_CHANGED, deviceName);
        context.sendBroadcast(intent);
    }

    private class UsbReceiver extends BroadcastReceiver {

        private static final String logTag = "UsbReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(logTag, "USB device connected");
                establishConnection();
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(logTag, "USB device disconnected");
                endConnection();
            }
        }
    }

    private void listenToSerial() {
        if (device != null && manager != null) {
            if (manager.hasPermission(device)) {
                startSerialConnection(manager, device);
            }
            else {
                Log.d("Serial", "Device doesn't have permissions");
            }
        }
    }

    private void stopListenToSerial() {
        if (serial != null) {
            stopSerialConnection(serial);
        }
    }

    private void startSerialConnection(UsbManager usbManager, UsbDevice device) {
        connection = usbManager.openDevice(device);
        serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

        if (serial != null && serial.open()) {
            serial.setBaudRate(9600);
            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serial.setParity(UsbSerialInterface.PARITY_NONE);
            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serial.read(readCB);
        }
    }

    private void stopSerialConnection(UsbSerialDevice serial) {
        if (serial != null && serial.open()) {
            serial.close();
            readCB = null;
        }
    }

    private final BroadcastReceiver usbReceiverPermission = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USE_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // call method to set up device communication
                        }
                    } else {
                        Log.d("ERROR", "permission denied for device " + device);
                    }
                }
            }
        }
    };
}
