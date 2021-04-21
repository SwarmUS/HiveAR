package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.adapters.ViewAgentListAdapter;
import com.swarmus.hivear.viewmodels.AgentListViewModel;

public class SwarmSummaryViewFragment extends Fragment {

    public static final String TAB_TITLE = "Agents";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swarm_summary_view_fragment, container, false);

        updateAgentListView(view);
        AgentListViewModel agentListViewModel = new ViewModelProvider(requireActivity()).get(AgentListViewModel.class);
        agentListViewModel.getAgentList().observe(requireActivity(), agents -> updateAgentListView(view));

        return view;
    }

    private void updateAgentListView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        if (recyclerView != null)
        {
            AgentListViewModel agentListViewModel = new ViewModelProvider(requireActivity()).get(AgentListViewModel.class);
            ViewAgentListAdapter viewAgentListAdapter = new ViewAgentListAdapter(agentListViewModel.getAgentList().getValue());
            recyclerView.setAdapter(viewAgentListAdapter);
            recyclerView.setHasFixedSize(true);
        }
    }
}
