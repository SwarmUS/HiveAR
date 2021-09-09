package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.swarmus.hivear.R;
import com.swarmus.hivear.viewmodels.TcpSettingsViewModel;

import java.util.Objects;

public class TcpSettingsFragment extends Fragment {
    private final String ip;
    private final int port;

    private TcpSettingsViewModel tcpSettingsViewModel;

    public TcpSettingsFragment(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tcp_settings, container, false);

        tcpSettingsViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(TcpSettingsViewModel.class);
        TextView ipText = view.findViewById(R.id.IPText);

        if (ipText != null) {
            ipText.setText(ip);
        }

        TextInputEditText portInputEditText = view.findViewById(R.id.PortTextInputEditText);
        if (portInputEditText!=null){
            portInputEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    String portInputValue = portInputEditText.getText().toString();
                    tcpSettingsViewModel.setPort(
                            portInputValue.isEmpty() ? 0 : Integer.parseInt(portInputValue));
                }
            });
            portInputEditText.setText(Integer.toString(port));

            portInputEditText.setOnFocusChangeListener((view1, b) -> {
                if (b) {portInputEditText.setSelection(portInputEditText.getText().length());}
            });
        }

        return view;
    }
}