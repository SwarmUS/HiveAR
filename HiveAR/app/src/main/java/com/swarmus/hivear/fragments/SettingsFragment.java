package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.viewmodels.AgentListViewModel;

public class SettingsFragment extends Fragment {

    public static final String TAB_TITLE = "April Tags";
    LinearLayout layoutList;
    AgentListViewModel agentListViewModel;
    private static final int EMPTY_ID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        agentListViewModel = new ViewModelProvider(requireActivity()).get(AgentListViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        layoutList = view.findViewById(R.id.configuration_container);
        FloatingActionButton addConfiguration = view.findViewById(R.id.add_configuration);

        addConfiguration.setOnClickListener(v-> {
            createNewConversionDialog(inflater, container).show();
        });

        // Create views from current list
        agentListViewModel.getIDConversions().forEach((aprilID, agentID)->addView(agentID, aprilID));

        return view;
    }

    private AlertDialog createNewConversionDialog(LayoutInflater inflater, ViewGroup container) {

        View dialogEdit = inflater.inflate(R.layout.dialog_view_new_conversion_config, container, false);

        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add conversion")
                .setMessage("Associate apriltag ID to Agent/Board ID")
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    if (addNewConversion(dialogEdit)) {
                        EditText agentIDET = dialogEdit.findViewById(R.id.edit_agent_id);
                        EditText aprilIDET = dialogEdit.findViewById(R.id.edit_agent_tag_id);

                        String agentIDStr = agentIDET.getText().toString();
                        String aprilIDStr = aprilIDET.getText().toString();

                        addView(Integer.valueOf(agentIDStr), Integer.valueOf(aprilIDStr));
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setView(dialogEdit)
                .show();

        return alertDialog;
    }

    private void addView(int boardIDValue, int aprilIDValue) {
        final View agentIDConfigView = getLayoutInflater().inflate(R.layout.row_add_configuration, null, false);

        TextView boardID = agentIDConfigView.findViewById(R.id.edit_agent_id);
        if (boardIDValue != EMPTY_ID) { boardID.setText(Integer.toString(boardIDValue)); }

        TextView aprilID = agentIDConfigView.findViewById(R.id.edit_agent_tag_id);
        if (aprilIDValue != EMPTY_ID) { aprilID.setText(Integer.toString(aprilIDValue)); }

        ImageView imageClose = agentIDConfigView.findViewById(R.id.edit_agent_id_delete);
        imageClose.setOnClickListener(view -> removeView(agentIDConfigView));

        layoutList.addView(agentIDConfigView);
    }

    private void removeView(View view) {
        // Do operations on agent here
        removeConversion(view);
        layoutList.removeView(view);
    }

    private boolean addNewConversion(View v) {
        EditText agentIDET = v.findViewById(R.id.edit_agent_id);
        EditText aprilIDET = v.findViewById(R.id.edit_agent_tag_id);

        String agentIDStr = agentIDET.getText().toString();
        String aprilIDStr = aprilIDET.getText().toString();

        if (!agentIDStr.isEmpty() && !aprilIDStr.isEmpty()) {
            // Notify user if couldn't add conversion
            if (!agentListViewModel.addNewConversion(Integer.valueOf(agentIDStr), Integer.valueOf(aprilIDStr))) {
                Toast.makeText(requireContext(), "Apriltag #" + aprilIDStr + " already defined", Toast.LENGTH_SHORT).show();
            }
            else {
                return true; // Was added
            }
        }
        else {
            Toast.makeText(requireActivity(), "Not added: Missing field", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void removeConversion(View v) {
        TextView aprilID = v.findViewById(R.id.edit_agent_tag_id);
        String aprilIDStr = aprilID.getText().toString();
        if (!aprilIDStr.isEmpty()) {
            agentListViewModel.removeConversion(Integer.valueOf(aprilIDStr));
        }
    }
}