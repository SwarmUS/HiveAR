package com.swarmus.hivear.models;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.enums.ConnectionStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Iterator;


public class SerialDevice extends CommunicationDevice {
    private PendingIntent permissionIntent;
    private UsbDevice device;
    private UsbManager manager;
    private UsbSerialDevice serial;

    private String selectedDeviceName;

    private PipedInputStream pipedInputStream;
    private PipedOutputStream pipedOutputStream;

    private ByteArrayOutputStream uartOutputStream;

    private static final String ACTION_USE_PERMISSION = "com.swarmus.hivear.USB_PERMISSION";
    private static final String DEVICE_INFO_LOG_TAG = "DeviceInformation";

    public static final String ACTION_SERIAL_DEVICE_CHANGED = "com.swarmus.hivear.SERIAL_DEVICE_CHANGED";
    public static final String EXTRA_SERIAL_DEVICE_CHANGED = "deviceName";

    public SerialDevice(Context context, ConnectionCallback connectionCallback) {
        super(context, connectionCallback);
        if (context != null)
        {
            uartOutputStream = new ByteArrayOutputStream();

            UsbReceiver usbReceiver = new UsbReceiver();
            IntentFilter usbDeviceFilter = new IntentFilter();
            usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            context.getApplicationContext().registerReceiver(usbReceiver, usbDeviceFilter);

            manager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
            permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                    ACTION_USE_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USE_PERMISSION);
            context.getApplicationContext().registerReceiver(usbReceiverPermission, filter);
            listConnectedDevices();
        }
    }

    @Override
    public void establishConnection() {
        endConnection();
        currentStatus = ConnectionStatus.connecting;
        broadCastConnectionStatus(currentStatus);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        device = deviceList.get(selectedDeviceName);
        listenToSerial();
    }

    @Override
    public void endConnection() {
        currentStatus = ConnectionStatus.notConnected;
        stopListenToSerial();
        setStreamsActive(false);
        if (connectionCallback != null) {
            connectionCallback.onDisconnect();
        }
    }

    @Override
    public void performConnectionCheck() {
        if (pipedOutputStream == null || uartOutputStream == null) {
            // Detect no more connection: end
            if ( currentStatus == ConnectionStatus.connected) {
                endConnection();
            }
        }
    }

    @Override
    public void sendData(byte[] data) {
        if (serial != null)
            synchronized (mutex) {
                serial.write(data);
            }
    }

    @Override
    public void sendData(String data) {
        byte[] msg = data.getBytes();
        sendData(msg);
    }

    @Override
    public void sendData(MessageOuterClass.Message protoMessage) {
        if (protoMessage != null && protoMessage.isInitialized())
        {
            synchronized (mutex) {
                try {
                    protoMessage.writeDelimitedTo(uartOutputStream);
                    sendData(uartOutputStream.toByteArray());
                    uartOutputStream = new ByteArrayOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public InputStream getDataStream() {
        return pipedInputStream;
    }

    public void setSelectedUsbDeviceName(String deviceName) {
        if (!deviceName.equals(selectedDeviceName)) { endConnection(); }
        selectedDeviceName = deviceName;
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        device = deviceList.get(selectedDeviceName);
        if (!manager.hasPermission(device)) {
            manager.requestPermission(device, permissionIntent);
        }
    }

    private class UsbReceiver extends BroadcastReceiver {
        private static final String USB_RECEIVER_LOG_TAG = "UsbReceiver";

        @Override
        public void onReceive(Context inputContext, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(USB_RECEIVER_LOG_TAG, "USB device connected");
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(USB_RECEIVER_LOG_TAG, "USB device disconnected");
            }
            listConnectedDevices();
            endConnection();
        }
    }

    private void listConnectedDevices()
    {
        if (manager!=null) {
            HashMap<String, String> devicesName = new HashMap<>();
            for (UsbDevice usbDevice : manager.getDeviceList().values()) {
                device = usbDevice;
                devicesName.put(device.getProductName(), device.getDeviceName());
            }

            logConnectedDevices();
            Intent connectedDevicesIntent = new Intent();
            connectedDevicesIntent.setAction(ACTION_SERIAL_DEVICE_CHANGED);
            connectedDevicesIntent.putExtra(EXTRA_SERIAL_DEVICE_CHANGED, devicesName);
            context.sendBroadcast(connectedDevicesIntent);
        }
    }

    private void listenToSerial() {
        if (device != null && manager != null) {
            if (manager.hasPermission(device)) {
                startSerialConnection(manager, device);
                return;
            }
            else {
                Log.d("Serial", "Device doesn't have permissions");
            }
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onConnectError();
    }

    private void stopListenToSerial() {
        if (serial != null) {
            stopSerialConnection(serial);
        }
    }

    private void startSerialConnection(UsbManager usbManager, UsbDevice device) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

        if (serial != null && serial.open()) {
            serial.setBaudRate(115200);
            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serial.setParity(UsbSerialInterface.PARITY_NONE);
            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serial.read(usbReadCallback);
            setStreamsActive(true);
            currentStatus = ConnectionStatus.connected;
            connectionCallback.onConnect();
        }
        else {
            setStreamsActive(false);
            currentStatus = ConnectionStatus.notConnected;
            connectionCallback.onConnectError();
        }
    }

    private void setStreamsActive(boolean active)
    {
        if (active)
        {
            pipedInputStream = new PipedInputStream();
            try {
                pipedOutputStream = new PipedOutputStream(pipedInputStream);
            } catch (IOException e) {
                Log.e(DEVICE_INFO_LOG_TAG, "Could not open pipe for UART data stream");
                e.printStackTrace();
            }
        }
        else {
            if (pipedOutputStream != null)
            {
                try {
                    pipedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pipedOutputStream = null;
            }
            if (pipedInputStream != null)
            {
                try {
                    pipedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pipedInputStream = null;
            }
        }
    }

    final UsbSerialInterface.UsbReadCallback usbReadCallback = (data) -> {
        try {
            if (pipedOutputStream == null) {
                endConnection();
            }
            pipedOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private void stopSerialConnection(UsbSerialDevice serial) {
        if (serial != null && serial.open()) {
            serial.close();
        }
    }

    private void logConnectedDevices() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        String i = "";
        while(deviceIterator.hasNext()) {
            UsbDevice iterDevice = deviceIterator.next();
            if (!manager.hasPermission(iterDevice)) {
                manager.requestPermission(iterDevice, permissionIntent);
            }
            i += "\n" + "DeviceID: " + iterDevice.getDeviceId() + "\n"
                    + "DeviceName: " + iterDevice.getDeviceName() + "\n"
                    + "DeviceClass: " + iterDevice.getDeviceClass() + " - "
                    + "DeviceSubClass: " + iterDevice.getDeviceSubclass() + "\n"
                    + "VendorID: " + iterDevice.getVendorId() + "\n"
                    + "ProductID: " + iterDevice.getProductId() + "\n"
                    + "Protocol: " + iterDevice.getDeviceProtocol() + "\n";
        }
        Log.d(DEVICE_INFO_LOG_TAG, i);
    }

    private final BroadcastReceiver usbReceiverPermission = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USE_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d("ERROR", "permission denied for device " + device);
                    }
                }
            }
        }
    };
}
