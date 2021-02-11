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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import static com.swarmus.hivear.utils.CRC.CalculateCRC32;
import static com.swarmus.hivear.utils.Serialization.bytesToInt16;
import static com.swarmus.hivear.utils.Serialization.bytesToInt32;
import static com.swarmus.hivear.utils.Serialization.int16ToBytes;


public class SerialDevice extends CommunicationDevice {
    private PendingIntent permissionIntent;
    private UsbDevice device;
    private UsbManager manager;
    private UsbSerialDevice serial;

    private String selectedDeviceName;

    private ByteArrayOutputStream rxByteStream;

    private int rxLength = 0;
    private int rxCrc = 0;

    private PipedInputStream pipedInputStream;
    private PipedOutputStream pipedOutputStream;

    private static final String ACTION_USE_PERMISSION = "com.swarmus.hivear.USB_PERMISSION";
    private static final String DEVICE_INFO_LOG_TAG = "DeviceInformation";

    public static final String ACTION_SERIAL_DEVICE_CHANGED = "com.swarmus.hivear.SERIAL_DEVICE_CHANGED";
    public static final String EXTRA_SERIAL_DEVICE_CHANGED = "deviceName";

    private static final int FIXED_HEADER_SIZE = 6;

    @Override
    public void init(Context context) {
        this.context = context;
        if (context != null)
        {
            rxByteStream = new ByteArrayOutputStream();

            pipedInputStream = new PipedInputStream();
            try {
                pipedOutputStream = new PipedOutputStream(pipedInputStream);
            } catch (IOException e) {
                Log.e(DEVICE_INFO_LOG_TAG, "Could open pipe for UART data stream");
                e.printStackTrace();
            }

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
        broadCastConnectionStatus(ConnectionStatus.connecting);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        device = deviceList.get(selectedDeviceName);
        listenToSerial();
    }

    @Override
    public void endConnection() {
        stopListenToSerial();
        broadCastConnectionStatus(ConnectionStatus.notConnected);
    }

    @Override
    public void sendData(byte[] data) {
        // TODO: Check for length limit and cut message into different packets if bigger than max size
        try {
            ByteArrayOutputStream msg = encodeMessage(data);
            byte[] msgBytes = msg.toByteArray();

            if (serial != null)
                serial.write(msgBytes);
        }
        catch (IOException e) {
            Log.e(DEVICE_INFO_LOG_TAG, "Error converting message to byte array");
            e.printStackTrace();
        }
    }

    @Override
    public void sendData(String data) {
        byte[] msg = data.getBytes();
        sendData(msg);
    }

    @Override
    public void sendData(MessageOuterClass.Message protoMessage) {
        // TODO
        /*if (protoMessage != null && protoMessage.isInitialized())
        {

        }*/
    }

    @Override
    public InputStream getDataStream() {
        return pipedInputStream;
    }

    public void setSelectedUsbDeviceName(String deviceName) {
        if (!deviceName.equals(selectedDeviceName)) { endConnection(); }
        selectedDeviceName = deviceName;
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

            Intent connectedDevicesIntent = new Intent();
            connectedDevicesIntent.setAction(ACTION_SERIAL_DEVICE_CHANGED);
            connectedDevicesIntent.putExtra(EXTRA_SERIAL_DEVICE_CHANGED, devicesName);
            context.sendBroadcast(connectedDevicesIntent);
            logConnectedDevices();
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
        broadCastConnectionStatus(ConnectionStatus.notConnected);
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
            broadCastConnectionStatus(ConnectionStatus.connected);
            serial.setBaudRate(115200);
            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serial.setParity(UsbSerialInterface.PARITY_NONE);
            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serial.read(usbReadCallback);
        }
        else {
            broadCastConnectionStatus(ConnectionStatus.notConnected);
        }
    }

    final UsbSerialInterface.UsbReadCallback usbReadCallback = (data) -> {
        try {
            rxByteStream.write(data);
            // TODO: Timeout

            // If header hasn't been analyzed and enough bytes have been received to do so
            if (rxByteStream.size() >= FIXED_HEADER_SIZE && rxLength == 0) {
                byte[] b = rxByteStream.toByteArray();
                rxLength = bytesToInt16(Arrays.copyOfRange(b, 0, 2));
                rxCrc = bytesToInt32(Arrays.copyOfRange(b, 2, 6));
            }

            // If we have received all bytes of a packet
            if (rxLength > 0 && rxByteStream.size() >= (rxLength + FIXED_HEADER_SIZE)) {
                byte[] b = rxByteStream.toByteArray();
                byte[] packet = Arrays.copyOfRange(b, FIXED_HEADER_SIZE, FIXED_HEADER_SIZE+rxLength);

                int calculatedCRC = bytesToInt32(CalculateCRC32(packet));

                // TODO: Send Ack/Nack depending on CRC
                if (calculatedCRC == rxCrc) {
                    Log.d(DEVICE_INFO_LOG_TAG, "CRC EQUAL");
                    pipedOutputStream.write(packet);
                } else {
                    Log.d(DEVICE_INFO_LOG_TAG, "CRC NOT EQUAL");
                }

                // Reset internal buffer and copy bytes that weren't part of the current packet into it
                rxByteStream = new ByteArrayOutputStream();
                rxByteStream.write(Arrays.copyOfRange(b, FIXED_HEADER_SIZE+rxLength, b.length));

                rxLength = 0;
                rxCrc = 0;
            }
        } catch (IOException e) {
            Log.e(DEVICE_INFO_LOG_TAG, "Error while receiving bytes from UART");
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
        device = deviceList.get(selectedDeviceName);
        listenToSerial();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        String i = "";
        while(deviceIterator.hasNext()) {
            device = deviceIterator.next();
            if (!manager.hasPermission(device)) {
                manager.requestPermission(device, permissionIntent);
            }
            i += "\n" + "DeviceID: " + device.getDeviceId() + "\n"
                    + "DeviceName: " + device.getDeviceName() + "\n"
                    + "DeviceClass: " + device.getDeviceClass() + " - "
                    + "DeviceSubClass: " + device.getDeviceSubclass() + "\n"
                    + "VendorID: " + device.getVendorId() + "\n"
                    + "ProductID: " + device.getProductId() + "\n"
                    + "Protocol: " + device.getDeviceProtocol() + "\n";
        }
        Log.d(DEVICE_INFO_LOG_TAG, i);
    }

    private static ByteArrayOutputStream encodeMessage(byte[] payload) throws IOException {
        byte[] length = int16ToBytes((short) payload.length);
        byte[] crc = CalculateCRC32(payload);

        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(length);
        msg.write(crc);
        msg.write(payload);

        return msg;
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
