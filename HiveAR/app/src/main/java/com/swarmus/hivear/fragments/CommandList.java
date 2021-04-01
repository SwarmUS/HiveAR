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
import com.swarmus.hivear.adapters.CommandsAdapter;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.viewmodels.CommandListVM;

import java.util.List;

public class CommandList extends Fragment {

    CommandListVM commandListVM;
    int destinationID;

    public CommandList(CommandListVM commandListVM, int destinationID) {
        this.commandListVM = commandListVM;
        this.destinationID = destinationID;
    }

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
    }

    private void updateCommands(List<FunctionTemplate> commandList) {
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