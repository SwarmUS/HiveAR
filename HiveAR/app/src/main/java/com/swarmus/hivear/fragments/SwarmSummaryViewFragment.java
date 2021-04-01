package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.adapters.ViewRobotListAdapter;
import com.swarmus.hivear.viewmodels.RobotListViewModel;

public class SwarmSummaryViewFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swarm_summary_view_fragment, container, false);

        updateRobotListView(view);
        RobotListViewModel robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);
        robotListViewModel.getRobotList().observe(requireActivity(), robots -> updateRobotListView(view));

        return view;
    }

    private void updateRobotListView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        if (recyclerView != null)
        {
            RobotListViewModel robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);
            ViewRobotListAdapter viewRobotListAdapter = new ViewRobotListAdapter(robotListViewModel.getRobotList().getValue());
            recyclerView.setAdapter(viewRobotListAdapter);
            recyclerView.setHasFixedSize(true);
        }
    }
}
