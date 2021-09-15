package com.swarmus.hivear.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import com.google.android.material.textfield.TextInputEditText;
import com.swarmus.hivear.R;
import com.swarmus.hivear.viewmodels.AgentListViewModel;
import com.swarmus.hivear.viewmodels.BoardNetworkConfigViewModel;

public class BoardNetworkConfig extends Fragment {
    public static final String TAB_TITLE = "Board Network";
    BoardNetworkConfigViewModel boardNetworkConfigViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardNetworkConfigViewModel = new ViewModelProvider(requireActivity()).get(BoardNetworkConfigViewModel.class);
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

        if (ssidInput != null)
        {
            ssidInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    String ssidInputValue = ssidInput.getText().toString();
                    boardNetworkConfigViewModel.setNetworkdSSID(ssidInputValue);
                }
            });

            ssidInput.setText(boardNetworkConfigViewModel.getNetworkSSID().getValue());
        }

        if (pswdInput != null)
        {
            pswdInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    String pswdInputValue = pswdInput.getText().toString();
                    boardNetworkConfigViewModel.setNetworkdPSWD(pswdInputValue);
                }
            });

            pswdInput.setText(boardNetworkConfigViewModel.getNetworkSSID().getValue());
        }

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