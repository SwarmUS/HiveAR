package com.swarmus.hivear.adapters;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.databinding.CommandArgumentBinding;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateArgument;

import java.util.List;

public class RobotCommandsAdapter extends RecyclerView.Adapter<RobotCommandsAdapter.RobotCommandsVH> {
    private final List<FunctionTemplate> commands;
    private Context context;
    private int swarmAgentDestinationID;
    private ViewGroup parentGroup;

    public RobotCommandsAdapter(@NonNull Context context, int swarmAgentDestinationID, List<FunctionTemplate> commands) {
        this.context = context;
        this.swarmAgentDestinationID = swarmAgentDestinationID;
        this.commands = commands;
    }

    @NonNull
    @Override
    public RobotCommandsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.command_card, parent, false);
        parentGroup = (ViewGroup)view;
        return new RobotCommandsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RobotCommandsVH holder, int position) {

        FunctionTemplate function = commands.get(position);
        holder.commandNameTV.setText(function.getName());
        holder.commandSendButton.setOnClickListener(view -> {
            ((MainActivity)context).sendCommand(function, swarmAgentDestinationID);
        });

        for (FunctionTemplateArgument arg : function.getArguments()) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            CommandArgumentBinding argViewBinding =
                    DataBindingUtil.inflate(inflater, R.layout.command_argument, parentGroup, false);
            argViewBinding.setFunctionArg(arg);

            EditText argInput = argViewBinding.getRoot().findViewById(R.id.command_argument_value);

            if (arg.getArgumentType().equals(Integer.class)) {
                argInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else if (arg.getArgumentType().equals(Float.class)) {
                argInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            } else {
                continue;
            }
            holder.commandArgumentList.addView(argViewBinding.getRoot());
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
