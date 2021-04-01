package com.swarmus.hivear.adapters;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;

class CommandsVH extends RecyclerView.ViewHolder {
    final TextView commandNameTV;
    final Button commandSendButton;
    final LinearLayout commandArgumentList;

    public CommandsVH(@NonNull View itemView) {
        super(itemView);

        commandNameTV = itemView.findViewById(R.id.command_name);
        commandSendButton = itemView.findViewById(R.id.command_send_button);
        commandArgumentList = itemView.findViewById(R.id.command_argument_list);
    }
}
