package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.TcpSettingsViewModel;

import java.util.Objects;

public class TcpSettingsFragment extends Fragment {
    private String ip;
    private int port;

    private TcpSettingsViewModel tcpSettingsViewModel;

    public TcpSettingsFragment(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tcp_settings, container, false);

        tcpSettingsViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(TcpSettingsViewModel.class);
        TextInputEditText ipInputEditText = view.findViewById(R.id.IPTextInputEditText);

        if (ipInputEditText != null) {
            // IP text input filtering
            InputFilter[] filters = new InputFilter[1];
            filters[0] = (source, start, end, dest, dstart, dend) -> {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String split : splits) {
                            if (Integer.parseInt(split) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            };
            ipInputEditText.setFilters(filters);

            ipInputEditText.setText(ip);

            ipInputEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    tcpSettingsViewModel.getIpAddress().setValue(Objects.requireNonNull(ipInputEditText.getText()).toString());
                }
            });

            ipInputEditText.setOnFocusChangeListener((view1, b) -> {
                if (b) {ipInputEditText.setSelection(ipInputEditText.getText().length());}
            });
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
                    tcpSettingsViewModel.getPort().setValue(
                            portInputValue.isEmpty() ?
                                    0 :
                                    Integer.parseInt(portInputValue)
                    );
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