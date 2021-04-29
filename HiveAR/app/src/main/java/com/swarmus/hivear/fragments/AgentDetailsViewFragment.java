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
import com.swarmus.hivear.commands.FetchAgentCommands;
import com.swarmus.hivear.models.Agent;
import com.swarmus.hivear.viewmodels.AgentBuzzCommandsVM;
import com.swarmus.hivear.viewmodels.AgentCommandsVM;
import com.swarmus.hivear.viewmodels.AgentListViewModel;

public class AgentDetailsViewFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.agent_details_view_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            AgentDetailsViewFragmentArgs args = AgentDetailsViewFragmentArgs.fromBundle(getArguments());

            FetchAgentCommands fetchAgentCommands = new FetchAgentCommands(args.getUid(), false);
            FetchAgentCommands fetchAgentBuzzCommands = new FetchAgentCommands(args.getUid(), true);

            TextView agentNameTV = view.findViewById(R.id.agent_name);
            agentNameTV.setText(args.getAgentname());
            TextView agentUIDTV = view.findViewById(R.id.agent_uid);
            agentUIDTV.setText(Integer.toString(args.getUid()));

            FloatingActionButton updateCommands = view.findViewById(R.id.updateCommands);
            updateCommands.setOnClickListener(v -> {
                ((MainActivity)requireActivity()).sendCommand(fetchAgentCommands);
                ((MainActivity)requireActivity()).sendCommand(fetchAgentBuzzCommands);
            });

            TabLayout tabLayout = view.findViewById(R.id.tabLayout);
            ViewPager viewPager = view.findViewById(R.id.viewPager);

            CommandViewPagerAdapter commandViewPagerAdapter = new CommandViewPagerAdapter(getChildFragmentManager());

            AgentListViewModel agentListViewModel = new ViewModelProvider(requireActivity()).get(AgentListViewModel.class);
            Agent agent = agentListViewModel.getAgentFromList(args.getUid());

            AgentCommandsVM agentCommandsVM = new ViewModelProvider(requireActivity()).get(AgentCommandsVM.class);
            AgentBuzzCommandsVM agentBuzzCommandsVM = new ViewModelProvider(requireActivity()).get(AgentBuzzCommandsVM.class);

            // Bind current agent's function to view functions
            agentCommandsVM.setList(agent.getCommands());
            agent.addObserver((observable, o) -> agentCommandsVM.setList(agent.getCommands()));

            agentBuzzCommandsVM.setList(agent.getBuzzCommands());
            agent.getCommands().addObserver(((observable, o) -> agentBuzzCommandsVM.setList(agent.getBuzzCommands())));

            commandViewPagerAdapter.addFragment(new CommandList(agentCommandsVM,
                            agent.getUid()),
                    agentCommandsVM.getListTitle());

            commandViewPagerAdapter.addFragment(new CommandList(agentBuzzCommandsVM,
                            agent.getUid()),
                    agentBuzzCommandsVM.getListTitle());

            viewPager.setAdapter(commandViewPagerAdapter);
            tabLayout.setupWithViewPager(viewPager);
        }
    }
}
