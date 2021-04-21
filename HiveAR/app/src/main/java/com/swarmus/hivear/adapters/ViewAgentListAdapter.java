package com.swarmus.hivear.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.fragments.CommandTabsDirections;
import com.swarmus.hivear.models.Agent;

import java.util.List;

public class ViewAgentListAdapter extends RecyclerView.Adapter<ViewAgentListAdapter.ViewAgentListVH> {

    private final List<Agent> agentList;

    public ViewAgentListAdapter(List<Agent> agentList) { this.agentList = agentList; }

    @NonNull
    @Override
    public ViewAgentListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.agent_list_row, parent, false);
        return new ViewAgentListVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewAgentListVH holder, int position) {

        Agent agent = agentList.get(position);
        holder.agentNameTV.setText(agent.getName());
        holder.agentUIDTV.setText(Integer.toString(agent.getUid()));
        holder.agentInfoLayout.setOnClickListener(view -> {

            CommandTabsDirections.ActionCommandTabsToAgentDetailsViewFragment action =
                    CommandTabsDirections.actionCommandTabsToAgentDetailsViewFragment();
            String agentName = agent.getName();
            action.setAgentname(agentName == null ? "" : agentName);
            action.setUid(agent.getUid());

            Navigation.findNavController(view).navigate(action);
        });
    }

    @Override
    public int getItemCount() { return agentList == null ? 0 : agentList.size(); }

    public static class ViewAgentListVH extends RecyclerView.ViewHolder {
        final TextView agentNameTV;
        final TextView agentUIDTV;
        final RelativeLayout agentInfoLayout;

        public ViewAgentListVH(@NonNull View itemView) {
            super(itemView);

            agentNameTV = itemView.findViewById(R.id.agent_name);
            agentUIDTV = itemView.findViewById(R.id.agent_uid);
            agentInfoLayout = itemView.findViewById(R.id.agent_info_layout);
        }
    }
}
