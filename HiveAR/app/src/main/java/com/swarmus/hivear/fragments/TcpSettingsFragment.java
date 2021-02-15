package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.TcpSettingsViewModel;

import java.util.Objects;

public class TcpSettingsFragment extends Fragment {

    private TcpSettingsViewModel tcpSettingsViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tcp_settings, container, false);

        tcpSettingsViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(TcpSettingsViewModel.class);

        TextInputEditText ipInputEditText = view.findViewById(R.id.IPTextInputEditText);
        ipInputEditText.setFocusableInTouchMode(true);
        ipInputEditText.setFocusable(true);
        ipInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    tcpSettingsViewModel.getIpAddress().setValue(Objects.requireNonNull(ipInputEditText.getText()).toString());
                }
            }
        });
        // IP text input filtering
        if (ipInputEditText != null) {
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

            ipInputEditText.setFocusableInTouchMode(true);
            ipInputEditText.setFocusable(true);
            ipInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        tcpSettingsViewModel.getIpAddress().setValue(Objects.requireNonNull(ipInputEditText.getText()).toString());
                        ipInputEditText.clearFocus();
                    }
                }
            });
        }

        TextInputEditText portInputEditText = view.findViewById(R.id.PortTextInputEditText);
        if (portInputEditText!=null){
            portInputEditText.setFocusableInTouchMode(true);
            portInputEditText.setFocusable(true);
            portInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        tcpSettingsViewModel.getPort().setValue(Integer.valueOf(Objects.requireNonNull(portInputEditText.getText()).toString()));
                        portInputEditText.clearFocus();
                    }
                }
            });
        }

        return view;
    }
}