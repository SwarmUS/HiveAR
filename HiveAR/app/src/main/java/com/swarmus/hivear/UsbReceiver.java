package com.swarmus.hivear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbReceiver extends BroadcastReceiver {

    private static final String logTag = "UsbReceiver";
    private static final String USB_CONNECTION_INTENT = "CONNECTION_INTENT";
    private static final String USB_DECONNECTION_INTENT = "DECONNECTION_INTENT";
    private DeviceActivity deviceActivity;

    public UsbReceiver() {
        this.deviceActivity = null;
    }

    public UsbReceiver(DeviceActivity deviceActivity) {
        this.deviceActivity = deviceActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            Log.d(logTag, "USB device connected");
            //deviceActivity.newUsbConnection();
            context.sendBroadcast(new Intent(USB_CONNECTION_INTENT));
        }
        else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            Log.d(logTag, "USB device disconnected");
            //deviceActivity.endUsbConnection();
            context.sendBroadcast(new Intent(USB_DECONNECTION_INTENT));
        }
    }
}

