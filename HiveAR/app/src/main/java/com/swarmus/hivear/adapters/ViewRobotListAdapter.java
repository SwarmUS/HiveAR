package com.swarmus.hivear.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.fragments.MoveByCommandFragment;
import com.swarmus.hivear.models.Robot;

import java.util.List;

// https://www.youtube.com/watch?v=pGi02uJre4M&ab_channel=AndroidWorldClub

public class ViewRobotListAdapter extends RecyclerView.Adapter<ViewRobotListAdapter.ViewRobotListVH> {
    public ViewRobotListAdapter(List<Robot> robotList) {
        this.robotList = robotList;
    }

    List<Robot> robotList;
    @NonNull
    @Override
    public ViewRobotListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.robot_list_row, parent, false);

        MoveByCommandFragment commandFragment = new MoveByCommandFragment();
        // TODO inflate fragment here
        return new ViewRobotListVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewRobotListVH holder, int position) {

        Robot robot = robotList.get(position);
        holder.robotNameTV.setText(robot.getName());
        holder.robotUIDTV.setText(Integer.toString(robot.getUid()));

        ArrayAdapter<String> adapter =  new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_dropdown_item, robot.getCommands());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.commandList.setAdapter(adapter);

        boolean isExpandable = robotList.get(position).isExpandable();
        holder.expandableLayout.setVisibility(isExpandable ? View.VISIBLE : View.GONE);

    }

    @Override
    public int getItemCount() {
        return robotList.size();
    }

    public class ViewRobotListVH extends RecyclerView.ViewHolder {
        TextView robotNameTV;
        TextView robotUIDTV;
        Spinner commandList;
        RelativeLayout robotInfoLayout;
        RelativeLayout expandableLayout;

        public ViewRobotListVH(@NonNull View itemView) {
            super(itemView);

            robotNameTV = itemView.findViewById(R.id.robot_name);
            robotUIDTV = itemView.findViewById(R.id.robot_uid);

            commandList = itemView.findViewById(R.id.commandList);

            robotInfoLayout = itemView.findViewById(R.id.robot_info_layout);
            expandableLayout = itemView.findViewById(R.id.expandable_layout);

            robotInfoLayout.setOnClickListener(view -> {
                Robot robot = robotList.get(getAdapterPosition());
                robot.setExpandable(!robot.isExpandable());
                notifyItemChanged(getAdapterPosition());
            });
        }
    }
}
