package com.swarmus.hivear;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class UartSettings extends Fragment {

    private TextView deviceNameTextView;

    private static String NO_DEVICE_FOUND = "No Device Found";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_uart_settings, container, false);
        deviceNameTextView = view.findViewById(R.id.serialDeviceInformation);
        setDeviceName((savedInstanceState != null) ? getArguments().getString("serialDeviceName") : null);
        return view;
    }

    @Nullable
    private void setDeviceName(@Nullable String deviceName) {
        if (deviceNameTextView != null) {
            if (deviceName != null) {
                deviceNameTextView.setText(deviceName);
                deviceNameTextView.setTextColor(getResources().getColor(R.color.design_default_color_on_primary));
            } else {
                deviceNameTextView.setText(NO_DEVICE_FOUND);
                deviceNameTextView.setTextColor(getResources().getColor(R.color.wrong_entry));
            }
        }
    }
}