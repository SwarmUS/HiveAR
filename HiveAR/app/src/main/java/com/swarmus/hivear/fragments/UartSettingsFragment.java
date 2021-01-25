package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.swarmus.hivear.R;

import java.util.Objects;

public class UartSettingsFragment extends Fragment {

    private TextView deviceNameTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_uart_settings, container, false);
        deviceNameTextView = view.findViewById(R.id.serialDeviceInformation);
        setDeviceName(getArguments() != null ? getArguments().getString("serialDeviceName") : null);
        return view;
    }

    public void setDeviceName(@Nullable String deviceName) {
        if (deviceNameTextView != null) {
            if (deviceName != null) {
                deviceNameTextView.setText(deviceName);
                deviceNameTextView.setTextColor(Objects.requireNonNull(getContext()).getColor(R.color.design_default_color_on_primary));
            } else {
                String NO_DEVICE_FOUND = "No Device Found";
                deviceNameTextView.setText(NO_DEVICE_FOUND);
                deviceNameTextView.setTextColor(Objects.requireNonNull(getContext()).getColor(R.color.wrong_entry));
            }
        }
    }
}