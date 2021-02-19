package com.swarmus.hivear.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.enums.CommandType;
import com.swarmus.hivear.factories.CommandArgumentViewFactory;

import java.util.List;

public class RobotCommandsAdapter extends RecyclerView.Adapter<RobotCommandsAdapter.RobotCommandsVH> {
    List<String> commands;

    public RobotCommandsAdapter(List<String> commands) {
        this.commands = commands;
    }

    @NonNull
    @Override
    public RobotCommandsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.command_card, parent, false);
        return new RobotCommandsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RobotCommandsVH holder, int position) {

        String command = commands.get(position);
        holder.commandNameTV.setText(command);
        holder.commandSendButton.setOnClickListener(view -> {

            // TODO sendData of proto msg constructed from args present in cardview.

        });
        for (View argView : CommandArgumentViewFactory.createCommandArgumentViews(LayoutInflater.from(holder.itemView.getContext()),
                CommandType.fromString(command))) {
            holder.commandArgumentList.addView(argView);
        }
    }

    @Override
    public int getItemCount() {
        if (commands != null) { return commands.size(); }
        else { return 0; }
    }

    public class RobotCommandsVH extends RecyclerView.ViewHolder {
        TextView commandNameTV;
        Button commandSendButton;
        LinearLayout commandArgumentList;

        public RobotCommandsVH(@NonNull View itemView) {
            super(itemView);

            commandNameTV = itemView.findViewById(R.id.command_name);
            commandSendButton = itemView.findViewById(R.id.command_send_button);
            commandArgumentList = itemView.findViewById(R.id.command_argument_list);
        }
    }
}
