package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.swarmus.hivear.R;
import com.swarmus.hivear.models.CurrentArRobotViewModel;
import com.swarmus.hivear.models.Robot;

public class ARViewFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ar_view_fragment, container, false);

        CurrentArRobotViewModel currentArRobotViewModel = new ViewModelProvider(requireActivity()).get(CurrentArRobotViewModel.class);
        currentArRobotViewModel.getSelectedRobot().observe(requireActivity(), robot -> setRobotUI(view, robot));

        setRobotUI(view, currentArRobotViewModel.getSelectedRobot().getValue());

        return view;
    }

    private void setRobotUI(View v, Robot robot) {
        Boolean isRobotSelected = robot != null;
        v.findViewById(R.id.robot_ar_selected).setVisibility(isRobotSelected ? LinearLayout.VISIBLE : LinearLayout.GONE);
        TextView robotName = v.findViewById(R.id.robot_ar_selected_name);
        robotName.setText(isRobotSelected ? robot.getName() : "");
        TextView robotUid = v.findViewById(R.id.robot_ar_selected_uid);
        robotUid.setText(isRobotSelected ? Integer.toString(robot.getUid()) : "");
    }
}
