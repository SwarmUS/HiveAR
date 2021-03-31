package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.adapters.RobotCommandsAdapter;
import com.swarmus.hivear.commands.FetchRobotCommands;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.viewmodels.RobotListViewModel;

public class RobotDetailsViewFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.robot_details_view_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            RobotDetailsViewFragmentArgs args = RobotDetailsViewFragmentArgs.fromBundle(getArguments());

            FetchRobotCommands fetchRobotCommands = new FetchRobotCommands(args.getUid());

            TextView robotNameTV = view.findViewById(R.id.robot_name);
            robotNameTV.setText(args.getRobotname());
            TextView robotUIDTV = view.findViewById(R.id.robot_uid);
            robotUIDTV.setText(Integer.toString(args.getUid()));

            FloatingActionButton updateCommands = view.findViewById(R.id.updateCommands);
            updateCommands.setOnClickListener(v -> {
                ((MainActivity)requireActivity()).sendCommand(fetchRobotCommands);
            });

            RecyclerView recyclerView = view.findViewById(R.id.robotDetailsRecycler);
            if (recyclerView != null)
            {
                // Maybe later on, replace List of String by list of Proto Requests for dynamic command UI inflation
                RobotListViewModel robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);
                Robot robot = robotListViewModel.getRobotFromList(args.getUid());
                if (robot != null) {
                    RobotCommandsAdapter robotCommandsAdapter =
                            new RobotCommandsAdapter(requireContext(), robot.getUid(), robot.getCommands());
                    recyclerView.setAdapter(robotCommandsAdapter);
                    recyclerView.setHasFixedSize(true);
                }
            }
        }
    }
}
