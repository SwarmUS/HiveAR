package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.adapters.SwarmCommandsAdapter;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.viewmodels.SwarmInfoViewModel;

import java.util.List;

public class SwarmCommands extends Fragment {

    public static final String TAB_TITLE = "Swarm";
    SwarmInfoViewModel swarmInfoViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swarmInfoViewModel = new ViewModelProvider(requireActivity()).get(SwarmInfoViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_swarm_commands, container, false);
        // TODO use this button when it will be possible to fetch swarm commands
        /*FloatingActionButton refreshCommands = view.findViewById(R.id.refreshCommands);
        refreshCommands.setOnClickListener(v -> {
            // TODO send fetch commands
            ((MainActivity)requireActivity()).sendCommand();
        });*/
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swarmInfoViewModel.getSwarmCommandList().observe(getViewLifecycleOwner(), this::udpateCommands);
        udpateCommands(swarmInfoViewModel.getSwarmCommandList().getValue());
    }

    private void udpateCommands(List<FunctionTemplate> commandList) {
        View view = getView();
        RecyclerView recyclerView = view.findViewById(R.id.swarmCommandsContainer);
        if (recyclerView != null)
        {
            if (commandList != null) {
                SwarmCommandsAdapter swarmCommandsAdapter =
                        new SwarmCommandsAdapter(requireContext(), commandList);
                recyclerView.setAdapter(swarmCommandsAdapter);
                recyclerView.setHasFixedSize(true);
            }
        }
    }
}