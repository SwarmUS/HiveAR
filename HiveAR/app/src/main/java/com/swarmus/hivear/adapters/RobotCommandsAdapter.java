package com.swarmus.hivear.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.FunctionCall;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.R;
import com.swarmus.hivear.factories.CommandArgumentViewFactory;

import java.util.List;

public class RobotCommandsAdapter extends RecyclerView.Adapter<RobotCommandsAdapter.RobotCommandsVH> {
    final List<MessageOuterClass.Request> commands;

    public RobotCommandsAdapter(List<MessageOuterClass.Request> commands) {
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

        FunctionCall.FunctionCallRequest function = commands.get(position).getUserCall().getFunctionCall();
        holder.commandNameTV.setText(function.getFunctionName());
        holder.commandSendButton.setOnClickListener(view -> {

            // TODO sendData of proto msg constructed from args present in cardview.

        });

        for (View argView : CommandArgumentViewFactory.createCommandArgumentViews(holder.itemView, function)) {
            holder.commandArgumentList.addView(argView);
        }
    }

    @Override
    public int getItemCount() {
        if (commands != null) { return commands.size(); }
        else { return 0; }
    }

    static class RobotCommandsVH extends RecyclerView.ViewHolder {
        final TextView commandNameTV;
        final Button commandSendButton;
        final LinearLayout commandArgumentList;

        public RobotCommandsVH(@NonNull View itemView) {
            super(itemView);

            commandNameTV = itemView.findViewById(R.id.command_name);
            commandSendButton = itemView.findViewById(R.id.command_send_button);
            commandArgumentList = itemView.findViewById(R.id.command_argument_list);
        }
    }
}
