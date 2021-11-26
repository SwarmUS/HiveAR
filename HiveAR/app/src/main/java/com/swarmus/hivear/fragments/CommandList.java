package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.adapters.CommandsAdapter;
import com.swarmus.hivear.models.FunctionTemplateList;
import com.swarmus.hivear.viewmodels.CommandListVM;

import java.util.List;

public class CommandList extends Fragment {

    CommandListVM commandListVM;
    int destinationID;
    boolean isBroadcast = false;
    private final  View.OnClickListener updateBehavior;

    public CommandList(CommandListVM commandListVM, int destinationID, View.OnClickListener updateBehavior) {
        this.commandListVM = commandListVM;
        this.destinationID = destinationID;
        this.updateBehavior = updateBehavior;
    }

    public void setBroadcastMode(boolean isBroadcast) {this.isBroadcast = isBroadcast;}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commandListVM = new ViewModelProvider(requireActivity()).get(commandListVM.getClass());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_command_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        commandListVM.getCommandList().observe(getViewLifecycleOwner(), this::updateCommands);
        updateCommands(commandListVM.getCommandList().getValue());
        FloatingActionButton updateButton = view.findViewById(R.id.update);
        if (updateButton != null) {
            updateButton.setOnClickListener(updateBehavior);
            updateButton.setVisibility(updateBehavior != null ? LinearLayout.VISIBLE : LinearLayout.GONE);
        }
    }

    private void updateCommands(FunctionTemplateList commandList) {
        View view = getView();
        RecyclerView recyclerView = view.findViewById(R.id.commandsContainer);
        if (recyclerView != null)
        {
            if (commandList != null) {
                CommandsAdapter commandsAdapter =
                        new CommandsAdapter(requireContext(), destinationID, commandList);
                recyclerView.setAdapter(commandsAdapter);
                recyclerView.setHasFixedSize(true);
            }
        }
    }
}