package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.swarmus.hivear.R;

public class RobotDetailsViewFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.robot_details_view_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            RobotDetailsViewFragmentArgs args = RobotDetailsViewFragmentArgs.fromBundle(getArguments());

            TextView robotNameTV = view.findViewById(R.id.robot_name);
            robotNameTV.setText(args.getRobotname());
            TextView robotUIDTV = view.findViewById(R.id.robot_uid);
            robotUIDTV.setText(Integer.toString(args.getUid()));

            // TODO set recycler view from commands (command factory) from args.getRobotCommands()
        }

    }
}
