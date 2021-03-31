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
import com.swarmus.hivear.models.Robot;

import java.util.List;

public class ViewRobotListAdapter extends RecyclerView.Adapter<ViewRobotListAdapter.ViewRobotListVH> {

    private final List<Robot> robotList;

    @NonNull
    @Override
    public ViewRobotListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.robot_list_row, parent, false);
        return new ViewRobotListVH(view);
    }

    public ViewRobotListAdapter(List<Robot> robotList) {
        this.robotList = robotList;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewRobotListVH holder, int position) {

        Robot robot = robotList.get(position);
        holder.robotNameTV.setText(robot.getName());
        holder.robotUIDTV.setText(Integer.toString(robot.getUid()));
        holder.robotInfoLayout.setOnClickListener(view -> {

            CommandTabsDirections.ActionCommandTabsToRobotDetailsViewFragment action =
                    CommandTabsDirections.actionCommandTabsToRobotDetailsViewFragment();
            String robotName = robot.getName();
            action.setRobotname(robotName == null ? "" : robotName);
            action.setUid(robot.getUid());

            Navigation.findNavController(view).navigate(action);
        });
    }

    @Override
    public int getItemCount() {
        if (robotList != null) { return robotList.size(); }
        else { return 0; }
    }

    public static class ViewRobotListVH extends RecyclerView.ViewHolder {
        final TextView robotNameTV;
        final TextView robotUIDTV;
        final RelativeLayout robotInfoLayout;

        public ViewRobotListVH(@NonNull View itemView) {
            super(itemView);

            robotNameTV = itemView.findViewById(R.id.robot_name);
            robotUIDTV = itemView.findViewById(R.id.robot_uid);

            robotInfoLayout = itemView.findViewById(R.id.robot_info_layout);
        }
    }
}
