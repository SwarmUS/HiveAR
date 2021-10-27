package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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
import com.swarmus.hivear.models.ProtoMsgStorer;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.TCPDeviceServer;
import com.swarmus.hivear.viewmodels.LocalSwarmAgentViewModel;
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

    private CheckBox detailedLogsCB;

    private static final int MSG_LOGGING_LENGTH = 10;
    private static final int MSG_SHORT_LOGGING_LENGTH = 30;
    private boolean isInfoVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.connection_view_fragment, container, false);

        protoMsgViewModel = new ViewModelProvider(requireActivity()).get(ProtoMsgViewModel.class);
        dataReceived = view.findViewById(R.id.dataReceived);
        detailedLogsCB = view.findViewById(R.id.detailedLogs);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRobotSelectionDropdown();
        initLoggerTextView();
        // Init filter dropdown
        filterChanged();

        initButtonCallbacks();

        // Update fragment
        FloatingActionButton switchCommunicationButton = view.findViewById(R.id.switchCommunication);
        updateCommunicationUI(switchCommunicationButton);
        expandUI(false); // Disable UI at start
    }

    private void initButtonCallbacks() {
        getView().findViewById(R.id.hide_ui).setOnClickListener(v -> {
            expandUI(!isInfoVisible);
        });

        detailedLogsCB.setOnCheckedChangeListener((compoundButton, checked) -> {
            filterChanged();
        });

        getView().findViewById(R.id.clean_text).setOnClickListener(v -> {
            protoMsgViewModel.clearLogs();
            dataReceived.setText("");
        });

        getView().findViewById(R.id.connectButton).setOnClickListener(v -> {
            CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
            if (communicationDevice != null) {
                communicationDevice.establishConnection();
            }
        });
        getView().findViewById(R.id.disconnectButton).setOnClickListener(v -> {
            CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
            if (communicationDevice != null) {
                communicationDevice.endConnection();
            }
        });

        FloatingActionButton switchCommunicationButton = getView().findViewById(R.id.switchCommunication);
        switchCommunicationButton.setOnClickListener(v -> {
            ((MainActivity)getActivity()).switchCurrentCommunicationDevice();
            updateCommunicationUI(switchCommunicationButton);
        });
    }

    private void initRobotSelectionDropdown() {
        Spinner dropdown = getView().findViewById(R.id.loggingFilter);
        protoMsgViewModel.getProtoMsgStorerList().observe(getViewLifecycleOwner(), storers -> {
            ArrayList<String> storerNames = new ArrayList<>();
            storers.forEach(protoMsgStorer -> storerNames.add(protoMsgStorer.getUniqueName()));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity().getBaseContext(), android.R.layout.simple_spinner_dropdown_item, storerNames.toArray(new String[0]));
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
                ProtoMsgStorer currentProtoMsgStorer = protoMsgViewModel.getCurrentProtoMsgStorer().getValue();
                LocalSwarmAgentViewModel localAgentVM = new ViewModelProvider(requireActivity()).get(LocalSwarmAgentViewModel.class);
                updateMoveByCommands( currentProtoMsgStorer != null &&
                        currentProtoMsgStorer.getAgentID() != localAgentVM.getLocalSwarmAgentID().getValue()?
                            protoMsgViewModel.getCurrentProtoMsgStorer().getValue().getAgentID() :
                            ProtoMsgStorer.NO_AGENT_ID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void initLoggerTextView() {
        dataReceived.setMovementMethod(new ScrollingMovementMethod());
        int logsCount = detailedLogsCB.isChecked() ? MSG_LOGGING_LENGTH : MSG_SHORT_LOGGING_LENGTH;
        dataReceived.setText(protoMsgViewModel.getLastMsgsSpannable(
                logsCount, detailedLogsCB.isChecked()));
        // Align text correctly on change logging filter
        protoMsgViewModel.getCurrentProtoMsgStorer().observe(getViewLifecycleOwner(), s -> filterChanged());
    }

    private void filterChanged() {
        if (protoMsgViewModel.getCurrentProtoMsgStorer().getValue() != null) {
            int logsCount = detailedLogsCB.isChecked() ? MSG_LOGGING_LENGTH : MSG_SHORT_LOGGING_LENGTH;
            dataReceived.setText(protoMsgViewModel.getLastMsgsSpannable(
                    logsCount, detailedLogsCB.isChecked()));

            // Align text on new message
            protoMsgViewModel.getCurrentProtoMsgStorer().getValue().addObserver((observable, object) -> {
                dataReceived.setText(protoMsgViewModel.getLastMsgsSpannable(logsCount,
                        detailedLogsCB.isChecked()));
            });
        }
    }

    private void updateMoveByCommands(int destinationID) {
        if (destinationID == ProtoMsgStorer.NO_AGENT_ID) {
            setMoveByVisible(false); // Always hide if no callbacks

            upCommand = null;
            getView().findViewById(R.id.upButton).setOnClickListener(null);

            downCommand = null;
            getView().findViewById(R.id.downButton).setOnClickListener(null);

            leftCommand = null;
            getView().findViewById(R.id.leftButton).setOnClickListener(null);

            rightCommand = null;
            getView().findViewById(R.id.rightButton).setOnClickListener(null);

            stopCommand = null;
            getView().findViewById(R.id.stopButton).setOnClickListener(null);
        } else {
            setMoveByVisible(isInfoVisible); // Show if current ui is expanded

            upCommand = new MoveByCommand(1,0, destinationID);
            getView().findViewById(R.id.upButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(upCommand));

            downCommand = new MoveByCommand(-1,0, destinationID);
            getView().findViewById(R.id.downButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(downCommand));

            leftCommand = new MoveByCommand(0,1, destinationID);
            getView().findViewById(R.id.leftButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(leftCommand));

            rightCommand = new MoveByCommand(0,-1, destinationID);
            getView().findViewById(R.id.rightButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(rightCommand));

            stopCommand = new MoveByCommand(0,0, destinationID);
            getView().findViewById(R.id.stopButton).setOnClickListener(v -> ((MainActivity)getActivity()).sendCommand(stopCommand));
        }
    }

    private void updateCommunicationUI(FloatingActionButton button) {
        CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
        Fragment currentFragment;
        if (communicationDevice instanceof SerialDevice) {
            currentFragment = new UsbSettingsFragment();
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.usb_icon));
        } else {
            currentFragment = new TcpSettingsFragment(((TCPDeviceServer)communicationDevice).getServerAddress(), ((TCPDeviceServer)communicationDevice).getServerPort());
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.wifi_icon));
        }
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.communicationContainer, currentFragment);
        ft.commit();
        // Invalidate view to update UI
        expandUI(true);
        getView().invalidate();
    }

    private void expandUI(boolean isVisible) {
        this.isInfoVisible = isVisible;

        FloatingActionButton hideUI = getView().findViewById(R.id.hide_ui);
        hideUI.setImageDrawable(ContextCompat.getDrawable(getContext(), isVisible ? R.drawable.open_full : R.drawable.close_full));

        setMoveByVisible(isVisible);
        setCommunicationContainerVisible(isVisible);

        getView().invalidate();
    }

    private void setMoveByVisible(boolean isVisible) {

        isVisible &= protoMsgViewModel.getCurrentProtoMsgStorer().getValue() != null &&
                     protoMsgViewModel.getCurrentProtoMsgStorer().getValue().getAgentID() != ProtoMsgStorer.NO_AGENT_ID;
        ConstraintLayout moveByLayout = getView().findViewById(R.id.moveByArrows);
        moveByLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        getView().invalidate();
    }

    private void setCommunicationContainerVisible(boolean isVisible) {
        FrameLayout fl = getView().findViewById(R.id.communicationContainer);
        fl.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        getView().invalidate();
    }
}
