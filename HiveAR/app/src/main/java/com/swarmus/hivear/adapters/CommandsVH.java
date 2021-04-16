package com.swarmus.hivear.adapters;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;

class CommandsVH extends RecyclerView.ViewHolder {
    final TextView commandNameTV;
    final Button commandSendButton;
    final LinearLayout commandArgumentList;
    final CardView cardView;
    final ImageView copyCommand;
    final ImageView deleteCommand;

    public CommandsVH(@NonNull View itemView) {
        super(itemView);

        cardView = itemView.findViewById(R.id.card_view);
        commandNameTV = itemView.findViewById(R.id.command_name);
        commandSendButton = itemView.findViewById(R.id.command_send_button);
        commandArgumentList = itemView.findViewById(R.id.command_argument_list);
        copyCommand = itemView.findViewById(R.id.copy_command);
        deleteCommand = itemView.findViewById(R.id.delete_command);
    }
}
