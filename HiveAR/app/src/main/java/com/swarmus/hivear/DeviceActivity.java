package com.swarmus.hivear;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDeviceConnection;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

public class DeviceActivity extends AppCompatActivity {
    PendingIntent permissionIntent;
    TextView deviceListingText;
    TextView deviceSerialData;
    TextView currentDevice;
    UsbDevice device;
    UsbManager manager;
    UsbDeviceConnection connection;
    UsbSerialDevice serial;
    UsbReceiver usbReceiver;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_USE_PERMISSION = "com.swarmus.usbcomm.USB_PERMISSION";
    private static final String NO_DEVICE_FOUND = "No device found";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        deviceListingText = findViewById(R.id.deviceListingText);
        deviceListingText.setMovementMethod(new ScrollingMovementMethod());
        deviceSerialData = findViewById(R.id.deviceSerialData);
        deviceSerialData.setMovementMethod(new ScrollingMovementMethod());
        currentDevice = findViewById(R.id.currentDevice);
        setNoDeviceInfo();

        FloatingActionButton toCommandButton = findViewById(R.id.toCommandButton);
        toCommandButton.setOnClickListener(view -> {
            Intent goToCommand = new Intent(getApplicationContext(), MainActivity.class);
            goToCommand.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(goToCommand);
        });

        Button clearDataButton = findViewById(R.id.clearSerialDataButton);
        clearDataButton.setOnClickListener(v -> deviceSerialData.setText(""));

        Button upButton = findViewById(R.id.upButton);
        upButton.setOnClickListener(v -> {
            if (serial != null) {
                byte[] data = ("Up"+'\n').getBytes();
                serial.write(data);
            }
        });
        Button leftButton = findViewById(R.id.leftButton);
        leftButton.setOnClickListener(v -> {
            if (serial != null) {
                byte[] data = ("Left"+'\n').getBytes();
                serial.write(data);
            }
        });
        Button rightButton = findViewById(R.id.rightButton);
        rightButton.setOnClickListener(v -> {
            if (serial != null) {
                byte[] data = ("Right"+'\n').getBytes();
                serial.write(data);
            }
        });
        Button downButton = findViewById(R.id.downButton);
        downButton.setOnClickListener(v -> {
            if (serial != null) {
                byte[] data = ("Down"+'\n').getBytes();
                serial.write(data);
            }
        });
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> {
            if (serial != null) {
                byte[] data = ("Stop"+'\n').getBytes();
                serial.write(data);
            }
        });

        usbReceiver = new UsbReceiver(this);
        IntentFilter usbDeviceFilter = new IntentFilter();
        usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, usbDeviceFilter);

        // Load devices
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USE_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USE_PERMISSION);
        registerReceiver(usbReceiverPermission, filter);

        newUsbConnection();
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

    void startSerialConnection(UsbManager usbManager, UsbDevice device) {
        connection = usbManager.openDevice(device);
        serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

        if (serial != null && serial.open()) {
            serial.setBaudRate(9600);
            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serial.setParity(UsbSerialInterface.PARITY_NONE);
            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serial.read(mCallback);
        }
    }

    void stopSerialConnection(UsbSerialDevice serial) {
        if (serial != null && serial.open()) {
            serial.close();
        }
    }

    public void write(byte[] data) {
        if (serial != null)
            serial.write(data);
    }

    UsbSerialInterface.UsbReadCallback mCallback = (data) -> {
        String dataStr = new String(data, StandardCharsets.UTF_8);
        if (deviceSerialData != null) {
            deviceSerialData.append(dataStr);
        }
        Log.i(TAG, "Data received: " + dataStr);
    };

    public void newUsbConnection() {
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
        deviceListingText.setText(i);
        Log.d("INFO", "I: " + i);
        if (device != null) {
            deviceSerialData.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.design_default_color_on_primary));
            currentDevice.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.design_default_color_on_primary));
            currentDevice.setText(device.getProductName());
            listenToSerial();
        }
        else {
            setNoDeviceInfo();
        }
    }

    public void endUsbConnection() {
        stopListenToSerial();
        setNoDeviceInfo();
    }

    private void setNoDeviceInfo() {
        deviceListingText.setText("");
        deviceSerialData.setText("");
        currentDevice.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.wrong_entry));
        currentDevice.setText(NO_DEVICE_FOUND);
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