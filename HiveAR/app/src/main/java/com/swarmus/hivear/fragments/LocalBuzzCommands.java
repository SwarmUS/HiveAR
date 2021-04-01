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
import com.swarmus.hivear.adapters.BroadcastCommandsAdapter;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.viewmodels.SwarmAgentInfoViewModel;

import java.util.List;

public class LocalBuzzCommands extends Fragment {
    public static final String TAB_TITLE = "Host buzz";

    SwarmAgentInfoViewModel swarmAgentInfoViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swarmAgentInfoViewModel = new ViewModelProvider(requireActivity()).get(SwarmAgentInfoViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local_buzz_commands, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swarmAgentInfoViewModel.getCommands().observe(getViewLifecycleOwner(), this::udpateCommands);
        udpateCommands(swarmAgentInfoViewModel.getCommands().getValue());
    }

    private void udpateCommands(List<FunctionTemplate> commandList) {
        View view = getView();
        RecyclerView recyclerView = view.findViewById(R.id.commandsContainer);
        if (recyclerView != null)
        {
            if (commandList != null) {
                BroadcastCommandsAdapter broadcastCommandsAdapter =
                        new BroadcastCommandsAdapter(requireContext(), commandList);
                recyclerView.setAdapter(broadcastCommandsAdapter);
                recyclerView.setHasFixedSize(true);
            }
        }
    }
}