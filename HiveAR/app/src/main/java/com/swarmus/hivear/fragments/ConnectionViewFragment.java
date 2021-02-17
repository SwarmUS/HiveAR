package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.commands.MoveByCommand;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.TCPDevice;

public class ConnectionViewFragment extends Fragment {
    private MoveByCommand upCommand;
    private MoveByCommand downCommand;
    private MoveByCommand leftCommand;
    private MoveByCommand rightCommand;
    private MoveByCommand stopCommand;

    private FragmentManager fragmentManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.connection_view_fragment, container, false);

        fragmentManager = getChildFragmentManager();

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
    }

    private void updateCommunicationUI(FloatingActionButton button) {
        CommunicationDevice communicationDevice = ((MainActivity)getActivity()).getCurrentCommunicationDevice();
        Fragment currentFragment;
        if (communicationDevice instanceof SerialDevice) {
            currentFragment = new UartSettingsFragment();
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.usb_icon));
        } else {
            currentFragment = new TcpSettingsFragment(((TCPDevice)communicationDevice).getServerIP(), ((TCPDevice)communicationDevice).getServerPort());
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.wifi_icon));
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.communicationContainer, currentFragment);
        ft.commit();
        // Invalidate view to update UI
        getView().invalidate();
        TextView dataReceived = getView().findViewById(R.id.dataReceived);
        if (dataReceived != null) {dataReceived.setText("");}
    }
}
