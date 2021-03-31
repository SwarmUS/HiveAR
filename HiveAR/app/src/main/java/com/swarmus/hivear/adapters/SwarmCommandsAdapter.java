package com.swarmus.hivear.adapters;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.databinding.CommandArgumentBinding;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateArgument;

import java.util.List;

public class SwarmCommandsAdapter extends RecyclerView.Adapter<CommandsVH>{
    private final List<FunctionTemplate> commands;
    private Context context;
    private ViewGroup parentGroup;

    public SwarmCommandsAdapter(@NonNull Context context, List<FunctionTemplate> commands) {
        this.context = context;
        this.commands = commands;
    }

    @NonNull
    @Override
    public CommandsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.command_card, parent, false);
        parentGroup = (ViewGroup)view;
        return new CommandsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommandsVH holder, int position) {

        FunctionTemplate function = commands.get(position);
        holder.commandNameTV.setText(function.getName());
        holder.commandSendButton.setOnClickListener(view -> {
            ((MainActivity)context).sendBuzzCommand(function);
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
        return commands.size();
    }
}
