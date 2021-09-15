package com.swarmus.hivear.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import com.google.android.material.textfield.TextInputEditText;
import com.swarmus.hivear.R;
public class BoardNetworkConfig extends Fragment {
    public static final String TAB_TITLE = "Board Network";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_board_network_config, container, false);

        Button configureBoardButton = v.findViewById(R.id.configureBoardNetwork);
        TextInputEditText ssidInput = v.findViewById(R.id.networkSSIDInputField);
        TextInputEditText pswdInput = v.findViewById(R.id.networkPSWDInputField);
        RadioGroup networkRole = v.findViewById(R.id.networkRole);

        configureBoardButton.setOnClickListener(view -> {
            // Send config to board, should be by usb, but look into that...

            if (configureBoardButton == null ||
                    ssidInput == null ||
                    pswdInput == null ||
                    networkRole == null)
            {
                return;
            }

            String ssid = "";
            if (ssidInput.getText() != null)
            {
                ssid = ssidInput.getText().toString();
            }

            String pswd = "";
            if (pswdInput.getText() != null)
            {
                pswd = ssidInput.getText().toString();
            }

            if (!ssid.isEmpty())
            {
                // send to board
                boolean isReceiver = networkRole.getCheckedRadioButtonId() == R.id.isReceiver;
            }
        });

        return v;
    }
}