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
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.adapters.CommandViewPagerAdapter;
import com.swarmus.hivear.commands.FetchRobotCommands;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.viewmodels.RobotBuzzCommandsVM;
import com.swarmus.hivear.viewmodels.RobotCommandsVM;
import com.swarmus.hivear.viewmodels.RobotListViewModel;

public class RobotDetailsViewFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.robot_details_view_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            RobotDetailsViewFragmentArgs args = RobotDetailsViewFragmentArgs.fromBundle(getArguments());

            FetchRobotCommands fetchRobotCommands = new FetchRobotCommands(args.getUid(), false);
            FetchRobotCommands fetchRobotBuzzCommands = new FetchRobotCommands(args.getUid(), true);

            TextView robotNameTV = view.findViewById(R.id.robot_name);
            robotNameTV.setText(args.getRobotname());
            TextView robotUIDTV = view.findViewById(R.id.robot_uid);
            robotUIDTV.setText(Integer.toString(args.getUid()));

            FloatingActionButton updateCommands = view.findViewById(R.id.updateCommands);
            updateCommands.setOnClickListener(v -> {
                ((MainActivity)requireActivity()).sendCommand(fetchRobotCommands);
                ((MainActivity)requireActivity()).sendCommand(fetchRobotBuzzCommands);
            });

            tabLayout = view.findViewById(R.id.tabLayout);
            viewPager = view.findViewById(R.id.viewPager);

            CommandViewPagerAdapter commandViewPagerAdapter = new CommandViewPagerAdapter(getChildFragmentManager());

            RobotListViewModel robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);
            Robot robot = robotListViewModel.getRobotFromList(args.getUid());

            RobotCommandsVM robotCommandsVM = new ViewModelProvider(requireActivity()).get(RobotCommandsVM.class);
            RobotBuzzCommandsVM robotBuzzCommandsVM = new ViewModelProvider(requireActivity()).get(RobotBuzzCommandsVM.class);

            // Bind current robot's function to view functions
            robotCommandsVM.getCommandList().setValue(robot.getCommands());
            robot.addObserver((observable, o) -> robotCommandsVM.getCommandList().setValue(robot.getCommands()));

            robotBuzzCommandsVM.getCommandList().setValue(robot.getBuzzCommands());
            robot.addObserver(((observable, o) -> robotBuzzCommandsVM.getCommandList().setValue(robot.getBuzzCommands())));

            commandViewPagerAdapter.addFragment(new CommandList(robotCommandsVM,
                            robot.getUid()),
                    robotCommandsVM.getListTitle());

            commandViewPagerAdapter.addFragment(new CommandList(robotBuzzCommandsVM,
                            robot.getUid()),
                    robotBuzzCommandsVM.getListTitle());

            viewPager.setAdapter(commandViewPagerAdapter);
            tabLayout.setupWithViewPager(viewPager);
        }
    }
}
