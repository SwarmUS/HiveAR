package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.swarmus.hivear.R;
import com.swarmus.hivear.viewmodels.SerialSettingsViewModel;

public class UsbSettingsFragment extends Fragment {

    private SerialSettingsViewModel serialSettingsViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usb_settings, container, false);

        serialSettingsViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.NewInstanceFactory()).get(SerialSettingsViewModel.class);

        Spinner dropdown = view.findViewById(R.id.serialDeviceInformation);
        serialSettingsViewModel.getDevices().observe(getViewLifecycleOwner(), devices -> {
            ArrayAdapter<String> adapter =  new ArrayAdapter<>(requireActivity().getBaseContext(), android.R.layout.simple_spinner_dropdown_item, devices.keySet().toArray(new String[0]));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dropdown.setAdapter(adapter);
            String selectedDevice = serialSettingsViewModel.getSelectedDevice().getValue();
            if (selectedDevice != null) {
                dropdown.setSelection(adapter.getPosition(selectedDevice));
            }
        });

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View currentView, int i, long l) {
                String selectedItem = adapterView.getItemAtPosition(i).toString();
                String deviceName = serialSettingsViewModel.getDevices().getValue().get(selectedItem);
                serialSettingsViewModel.setSelectedDevice(deviceName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        return view;
    }
}