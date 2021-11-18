package com.swarmus.hivear.adapters;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;
import com.swarmus.hivear.activities.MainActivity;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateArgument;
import com.swarmus.hivear.models.FunctionTemplateList;

public class ARCommandsAdapter extends RecyclerView.Adapter<ARCommandsAdapter.ARCommandsVH> {
    private final FunctionTemplateList functionTemplateList;
    private final FunctionTemplateList buzzFunctionTemplateList;
    private Context context;
    private int destinationId;

    public ARCommandsAdapter(@NonNull Context context,
                             int destinationId,
                             FunctionTemplateList functionTemplateList,
                             FunctionTemplateList buzzFunctionTemplateList) {
        this.context = context;
        this.destinationId = destinationId;
        this.functionTemplateList = functionTemplateList;
        this.buzzFunctionTemplateList = buzzFunctionTemplateList;
    }

    @NonNull
    @Override
    public ARCommandsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ar_command_card, parent, false);
        return new ARCommandsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ARCommandsVH holder, int position) {
        if (functionTemplateList == null && functionTemplateList == null) {return;}

        FunctionTemplate function = null;
        if (functionTemplateList != null) {
            if (position < functionTemplateList.size()) {
                function = functionTemplateList.at(position);
            }
            else {
                function = buzzFunctionTemplateList.at(position - functionTemplateList.size());
            }
        }
        else {
            function = buzzFunctionTemplateList.at(position);
        }

        holder.commandNameTV.setText(function.getName());
        holder.commandNameTV.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                function.isBuzzFunction() ? R.drawable.ic_buzz : 0,
                0);

        FunctionTemplate finalFunction = function;
        holder.commandSendButton.setOnClickListener(view -> {
            ((MainActivity)context).sendCommand(finalFunction, destinationId);
        });

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (FunctionTemplateArgument arg : function.getArguments()) {
            com.swarmus.hivear.databinding.ArCommandArgumentBinding argViewBinding =
                    DataBindingUtil.inflate(inflater, R.layout.ar_command_argument, holder.cardView, false);
            argViewBinding.setFunctionArg(arg);

            TextView argInput = argViewBinding.getRoot().findViewById(R.id.command_argument_value);
            FloatingActionButton decrease = argViewBinding.decreaseValue;
            decrease.setOnClickListener(v -> {
                if (arg.getArgumentType().equals(Integer.class)) {
                    int value = Integer.parseInt(arg.getValue());
                    argInput.setText(String.valueOf(value - 1));
                }

                if (arg.getArgumentType().equals(Float.class)) {
                    float value = Float.parseFloat(arg.getValue());
                    argInput.setText(String.valueOf(value - 0.5));
                }
            });

            FloatingActionButton increase = argViewBinding.increaseValue;
            increase.setOnClickListener(v -> {
                if (arg.getArgumentType().equals(Integer.class)) {
                    int value = Integer.parseInt(arg.getValue());
                    argInput.setText(String.valueOf(value + 1));
                }

                if (arg.getArgumentType().equals(Float.class)) {
                    float value = Float.parseFloat(arg.getValue());
                    argInput.setText(String.valueOf(value + 0.5));
                }
            });

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
        int count = 0;
        count += functionTemplateList != null ? functionTemplateList.size() : 0;
        count += buzzFunctionTemplateList != null ? buzzFunctionTemplateList.size() : 0;
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public class ARCommandsVH extends RecyclerView.ViewHolder {
        final TextView commandNameTV;
        final Button commandSendButton;
        final LinearLayout commandArgumentList;
        final CardView cardView;

        public ARCommandsVH(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_view);
            commandNameTV = itemView.findViewById(R.id.command_name);
            commandSendButton = itemView.findViewById(R.id.command_send_button);
            commandArgumentList = itemView.findViewById(R.id.command_argument_list);
        }
    }
}
