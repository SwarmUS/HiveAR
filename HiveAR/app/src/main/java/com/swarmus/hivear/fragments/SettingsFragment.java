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
import com.swarmus.hivear.viewmodels.RobotListViewModel;

public class SettingsFragment extends Fragment {

    LinearLayout layoutList;
    RobotListViewModel robotListViewModel;
    private static final int EMPTY_ID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);
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
        robotListViewModel.getIDConversions().forEach((aprilID, robotID)->addView(robotID, aprilID));

        return view;
    }

    private AlertDialog createNewConversionDialog(LayoutInflater inflater, ViewGroup container) {

        View dialogEdit = inflater.inflate(R.layout.dialog_view_new_conversion_config, container, false);

        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add conversion")
                .setMessage("Associate apriltag ID to Robot/Board ID")
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    if (addNewConversion(dialogEdit)) {
                        EditText robotIDET = dialogEdit.findViewById(R.id.edit_robot_id);
                        EditText aprilIDET = dialogEdit.findViewById(R.id.edit_robot_tag_id);

                        String robotIDStr = robotIDET.getText().toString();
                        String aprilIDStr = aprilIDET.getText().toString();

                        addView(Integer.valueOf(robotIDStr), Integer.valueOf(aprilIDStr));
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setView(dialogEdit)
                .show();

        return alertDialog;
    }

    private void addView(int boardIDValue, int aprilIDValue) {
        final View robotIDConfigView = getLayoutInflater().inflate(R.layout.row_add_configuration, null, false);

        TextView boardID = robotIDConfigView.findViewById(R.id.edit_robot_id);
        if (boardIDValue != EMPTY_ID) { boardID.setText(Integer.toString(boardIDValue)); }

        TextView aprilID = robotIDConfigView.findViewById(R.id.edit_robot_tag_id);
        if (aprilIDValue != EMPTY_ID) { aprilID.setText(Integer.toString(aprilIDValue)); }

        ImageView imageClose = robotIDConfigView.findViewById(R.id.edit_robot_id_delete);
        imageClose.setOnClickListener(view -> removeView(robotIDConfigView));

        layoutList.addView(robotIDConfigView);
    }

    private void removeView(View view) {
        // Do operations on robot here
        removeConversion(view);
        layoutList.removeView(view);
    }

    private boolean addNewConversion(View v) {
        EditText robotIDET = v.findViewById(R.id.edit_robot_id);
        EditText aprilIDET = v.findViewById(R.id.edit_robot_tag_id);

        String robotIDStr = robotIDET.getText().toString();
        String aprilIDStr = aprilIDET.getText().toString();

        if (!robotIDStr.isEmpty() && !aprilIDStr.isEmpty()) {
            // Notify user if couldn't add conversion
            if (!robotListViewModel.addNewConversion(Integer.valueOf(robotIDStr), Integer.valueOf(aprilIDStr))) {
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
        TextView aprilID = v.findViewById(R.id.edit_robot_tag_id);
        String aprilIDStr = aprilID.getText().toString();
        if (!aprilIDStr.isEmpty()) {
            robotListViewModel.removeConversion(Integer.valueOf(aprilIDStr));
        }
    }
}