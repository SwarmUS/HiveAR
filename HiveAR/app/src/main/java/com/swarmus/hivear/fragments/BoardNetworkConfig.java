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
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.commands.SendNetworkConfigComand;
import com.swarmus.hivear.viewmodels.AgentListViewModel;
import com.swarmus.hivear.viewmodels.BoardNetworkConfigViewModel;
import com.swarmus.hivear.viewmodels.LocalSwarmAgentViewModel;

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
        CheckBox isRouter = v.findViewById(R.id.isRouter);
        CheckBox isNetworkMesh = v.findViewById(R.id.isNetworkMesh);

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

            pswdInput.setText(boardNetworkConfigViewModel.getNetworkPSWD().getValue());
        }

        configureBoardButton.setOnClickListener(view -> {
            // Send config to board, should be by usb, but look into that...

            if (configureBoardButton == null ||
                    ssidInput == null ||
                    pswdInput == null ||
                    isRouter == null ||
                    isNetworkMesh == null)
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
                LocalSwarmAgentViewModel localSwarmAgentViewModel =
                        new ViewModelProvider(requireActivity()).get(LocalSwarmAgentViewModel.class);

                int localId = localSwarmAgentViewModel.getLocalSwarmAgentID().getValue();
                // If invalid, don't send
                if (localId != LocalSwarmAgentViewModel.DEFAULT_SWARM_AGENT_ID)
                {
                    SendNetworkConfigComand sendNetworkConfigComand = new SendNetworkConfigComand(
                            localSwarmAgentViewModel.getLocalSwarmAgentID().getValue(),
                            ssid,
                            pswd,
                            isRouter.isChecked(),
                            isNetworkMesh.isChecked());

                    ((MainActivity)requireActivity()).sendCommand(sendNetworkConfigComand);
                }
                else
                {
                    Toast.makeText(requireContext(), "Board not identified, couldn't send config", Toast.LENGTH_SHORT).show();
                }

            }
            else
            {
                Toast.makeText(requireContext(), "Need at least an SSID to configure", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }
}