package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.commands.MoveByCommand;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.TCPDeviceServer;
import com.swarmus.hivear.viewmodels.ProtoMsgViewModel;

import java.util.ArrayList;

public class ConnectionViewFragment extends Fragment {
    private MoveByCommand upCommand;
    private MoveByCommand downCommand;
    private MoveByCommand leftCommand;
    private MoveByCommand rightCommand;
    private MoveByCommand stopCommand;

    private ProtoMsgViewModel protoMsgViewModel;
    private TextView dataReceived;

    private FragmentManager fragmentManager;
    private static final int MSG_LOGGING_LENGTH = 10;
    private boolean isInfoVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.connection_view_fragment, container, false);

        fragmentManager = getChildFragmentManager();

        protoMsgViewModel = new ViewModelProvider(requireActivity()).get(ProtoMsgViewModel.class);

        Spinner dropdown = view.findViewById(R.id.loggingFilter);
        protoMsgViewModel.getProtoMsgStorerList().observe(getViewLifecycleOwner(), storers -> {
            ArrayList<String> storerNames = new ArrayList<>();
            storers.forEach(protoMsgStorer -> storerNames.add(protoMsgStorer.getUniqueName()));
            ArrayAdapter<String> adapter =  new ArrayAdapter<>(requireActivity().getBaseContext(), android.R.layout.simple_spinner_dropdown_item, storerNames.toArray(new String[0]));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dropdown.setAdapter(adapter);
            if (protoMsgViewModel.getCurrentProtoMsgStorer().getValue() != null) {
                String selectedDevice = protoMsgViewModel.getCurrentProtoMsgStorer().getValue().getUniqueName();
                if (selectedDevice != null) {
                    dropdown.setSelection(adapter.getPosition(selectedDevice));
                }
            }
        });
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View currentView, int i, long l) {
                protoMsgViewModel.setCurrentProtoMsgStorer( protoMsgViewModel.getProtoMsgStorerList().getValue().get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        dataReceived = view.findViewById(R.id.dataReceived);
        dataReceived.setMovementMethod(new ScrollingMovementMethod());

        dataReceived.setText(protoMsgViewModel.getLastMsgsSpannable(MSG_LOGGING_LENGTH));
        // Align text correctly on change logging filter
        protoMsgViewModel.getCurrentProtoMsgStorer().observe(getViewLifecycleOwner(), s -> { filterChanged(); });

        // Init filter dropdown
        filterChanged();

        view.findViewById(R.id.hide_ui).setOnClickListener(v -> {
            setInfoVisible(!isInfoVisible);
        });

        view.findViewById(R.id.clean_text).setOnClickListener(v -> {
            protoMsgViewModel.clearLogs();
            dataReceived.setText("");
        });

        view.findViewById(R.id.connectButton).setOnClickListener(v -> {
            CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
            if (communicationDevice != null) {
                communicationDevice.establishConnection();
            }
        });
        view.findViewById(R.id.disconnectButton).setOnClickListener(v -> {
            CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
            if (communicationDevice != null) {
                communicationDevice.endConnection();
            }
        });

        FloatingActionButton switchCommunicationButton = view.findViewById(R.id.switchCommunication);
        switchCommunicationButton.setOnClickListener(v -> {
            ((MainActivity)getActivity()).switchCurrentCommunicationDevice();
            updateCommunicationUI(switchCommunicationButton);
        });

        upCommand = new MoveByCommand(1,0);
        view.findViewById(R.id.upButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(upCommand));

        downCommand = new MoveByCommand(-1,0);
        view.findViewById(R.id.downButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(downCommand));

        leftCommand = new MoveByCommand(0,1);
        view.findViewById(R.id.leftButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(leftCommand));

        rightCommand = new MoveByCommand(0,-1);
        view.findViewById(R.id.rightButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(rightCommand));

        stopCommand = new MoveByCommand(0,0);
        view.findViewById(R.id.stopButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(stopCommand));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Update fragment
        FloatingActionButton switchCommunicationButton = view.findViewById(R.id.switchCommunication);
        updateCommunicationUI(switchCommunicationButton);
        setInfoVisible(false); // Disable UI at start
    }

    private void filterChanged() {
        if (protoMsgViewModel.getCurrentProtoMsgStorer().getValue() != null) {
            dataReceived.setText(protoMsgViewModel.getLastMsgsSpannable(MSG_LOGGING_LENGTH));

            // Align text on new message
            protoMsgViewModel.getCurrentProtoMsgStorer().getValue().addObserver((observable, object) -> {
                dataReceived.setText(protoMsgViewModel.getLastMsgsSpannable(MSG_LOGGING_LENGTH));
            });
        }
    }

    private void updateCommunicationUI(FloatingActionButton button) {
        CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
        Fragment currentFragment;
        if (communicationDevice instanceof SerialDevice) {
            currentFragment = new UartSettingsFragment();
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.usb_icon));
        } else {
            currentFragment = new TcpSettingsFragment(((TCPDeviceServer)communicationDevice).getServerAddress(), ((TCPDeviceServer)communicationDevice).getServerPort());
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.wifi_icon));
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.communicationContainer, currentFragment);
        ft.commit();
        // Invalidate view to update UI
        setInfoVisible(true);
        getView().invalidate();
    }

    private void setInfoVisible(boolean isVisible) {
        this.isInfoVisible = isVisible;

        View v = getView();

        FloatingActionButton hideUI = v.findViewById(R.id.hide_ui);
        hideUI.setImageDrawable(ContextCompat.getDrawable(getContext(), isVisible ? R.drawable.open_full : R.drawable.close_full));

        ConstraintLayout moveByLayout = v.findViewById(R.id.moveByArrows);
        moveByLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);

        FrameLayout fl = v.findViewById(R.id.communicationContainer);
        fl.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        v.invalidate();
    }
}
