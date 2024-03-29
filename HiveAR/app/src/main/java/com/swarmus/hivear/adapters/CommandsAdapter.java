package com.swarmus.hivear.adapters;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.databinding.CommandArgumentBinding;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateArgument;
import com.swarmus.hivear.models.FunctionTemplateList;
import com.swarmus.hivear.viewmodels.BroadcastInfoViewModel;

import java.util.List;

public class CommandsAdapter extends RecyclerView.Adapter<CommandsAdapter.CommandsVH> {
    private final FunctionTemplateList functionTemplateList;
    private Context context;
    private int destinationId;
    private ViewGroup parentGroup;
    private final BroadcastInfoViewModel broadcastInfoViewModel;

    public CommandsAdapter(@NonNull Context context,
                           int destinationId,
                           FunctionTemplateList functionTemplateList) {
        this.context = context;
        this.destinationId = destinationId;
        this.functionTemplateList = functionTemplateList;
        this.broadcastInfoViewModel = new ViewModelProvider((ViewModelStoreOwner)context).get(BroadcastInfoViewModel.class);
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
        if (functionTemplateList == null) {return;}

        FunctionTemplate function = functionTemplateList.at(position);

        holder.cardView.setOnLongClickListener(view -> {
            // Open add function popup
            String alertMsg = String.format("Add \"%s\" function to broadcast list?", function.getName());
            new AlertDialog.Builder(context)
                    .setTitle("Add broadcast function")
                    .setMessage(alertMsg)
                    .setPositiveButton("Yes", (dialog, whichButton) -> {
                        if (broadcastInfoViewModel != null) {
                            broadcastInfoViewModel.addFunction(function);
                        }
                    }).setNegativeButton("No", (dialog, whichButton) -> {
                // Do nothing.
            }).show();
            return true;
        });

        holder.commandNameTV.setText(function.getName());
        holder.commandNameTV.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                function.isBuzzFunction() ? R.drawable.ic_buzz : 0,
                0);

        holder.commandSendButton.setOnClickListener(view -> {
            ((MainActivity)context).sendCommand(function, destinationId);
        });

        holder.copyCommand.setOnClickListener(view -> {
            // Open copy function popup
            String alertMsg = String.format("Duplicate function \"%s\"?", function.getName());
            new AlertDialog.Builder(context)
                    .setTitle("Duplicate function")
                    .setMessage(alertMsg)
                    .setPositiveButton("Yes", (dialog, whichButton) -> {
                        if (functionTemplateList != null) {
                            functionTemplateList.addDuplicate(function);
                        }
                    }).setNegativeButton("No", (dialog, whichButton) -> {
                // Do nothing.
            }).show();
        });

        holder.deleteCommand.setOnClickListener(view -> {
            // Open delete function popup
            String alertMsg = String.format("Delete function \"%s\"?", function.getName());
            new AlertDialog.Builder(context)
                    .setTitle("Delete function")
                    .setMessage(alertMsg)
                    .setPositiveButton("Yes", (dialog, whichButton) -> {
                        if (functionTemplateList != null) {
                            functionTemplateList.remove(function);
                        }
                    }).setNegativeButton("No", (dialog, whichButton) -> {
                // Do nothing.
            }).show();
        });

        for (FunctionTemplateArgument arg : function.getArguments()) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            CommandArgumentBinding argViewBinding =
                    DataBindingUtil.inflate(inflater, R.layout.command_argument, parentGroup, false);
            argViewBinding.setFunctionArg(arg);

            EditText argInput = argViewBinding.getRoot().findViewById(R.id.command_argument_value);

            if (arg.getArgumentType().equals(Integer.class)) {
                argInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            } else if (arg.getArgumentType().equals(Float.class)) {
                argInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            } else {
                continue;
            }
            holder.commandArgumentList.addView(argViewBinding.getRoot());
        }
    }

    @Override
    public int getItemCount() {
        if (functionTemplateList != null) {
            return functionTemplateList.size();
        }
        else { return 0; }
    }

    public class CommandsVH extends RecyclerView.ViewHolder {
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
}
