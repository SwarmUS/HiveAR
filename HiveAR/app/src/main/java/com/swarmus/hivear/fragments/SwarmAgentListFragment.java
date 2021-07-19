package com.swarmus.hivear.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.adapters.ViewAgentListAdapter;
import com.swarmus.hivear.commands.UpdateAgentsList;
import com.swarmus.hivear.viewmodels.AgentListViewModel;
public class SwarmAgentListFragment extends Fragment {

    public static final String TAB_TITLE = "Agents";
    private AgentListViewModel agentListViewModel;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swarm_agent_list_fragment, container, false);

        updateAgentListView(view);
        agentListViewModel = new ViewModelProvider(requireActivity()).get(AgentListViewModel.class);
        agentListViewModel.getAgentList().observe(requireActivity(), agents -> updateAgentListView(view));

        FloatingActionButton updateAgentsListButton = view.findViewById(R.id.updateAgentList);
        UpdateAgentsList updateAgentsList = new UpdateAgentsList();
        updateAgentsListButton.setOnClickListener(v -> {
            ((MainActivity)requireActivity()).sendCommand(updateAgentsList);
        });

        return view;
    }

    private void updateAgentListView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        if (recyclerView != null && agentListViewModel != null)
        {
            ViewAgentListAdapter viewAgentListAdapter = new ViewAgentListAdapter(agentListViewModel.getAgentList().getValue());
            recyclerView.setAdapter(viewAgentListAdapter);
            recyclerView.setHasFixedSize(true);
        }
    }
}
