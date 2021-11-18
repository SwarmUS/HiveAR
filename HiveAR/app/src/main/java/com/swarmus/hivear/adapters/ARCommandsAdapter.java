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
import com.swarmus.hivear.databinding.ArCommandArgumentBinding;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateArgument;
import com.swarmus.hivear.models.FunctionTemplateList;
import com.swarmus.hivear.viewmodels.BroadcastInfoViewModel;

public class ARCommandsAdapter extends RecyclerView.Adapter<ARCommandsAdapter.ARCommandsVH> {
    private final FunctionTemplateList functionTemplateList;
    private Context context;
    private int destinationId;
    private ViewGroup parentGroup;
    private final BroadcastInfoViewModel broadcastInfoViewModel;

    public ARCommandsAdapter(@NonNull Context context,
                             int destinationId,
                             FunctionTemplateList functionTemplateList) {
        this.context = context;
        this.destinationId = destinationId;
        this.functionTemplateList = functionTemplateList;
        this.broadcastInfoViewModel = new ViewModelProvider((ViewModelStoreOwner)context).get(BroadcastInfoViewModel.class);
    }

    @NonNull
    @Override
    public ARCommandsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ar_command_card, parent, false);
        parentGroup = (ViewGroup)view;
        return new ARCommandsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ARCommandsVH holder, int position) {
        if (functionTemplateList == null) {return;}

        FunctionTemplate function = functionTemplateList.at(position);

        holder.commandNameTV.setText(function.getName());
        holder.commandNameTV.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                function.isBuzzFunction() ? R.drawable.ic_buzz : 0,
                0);

        holder.commandSendButton.setOnClickListener(view -> {
            ((MainActivity)context).sendCommand(function, destinationId);
        });

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (FunctionTemplateArgument arg : function.getArguments()) {
            com.swarmus.hivear.databinding.ArCommandArgumentBinding argViewBinding =
                    DataBindingUtil.inflate(inflater, R.layout.ar_command_argument, holder.cardView, false);
            argViewBinding.setFunctionArg(arg);

            EditText argInput = argViewBinding.getRoot().findViewById(R.id.command_argument_value);
            argInput.requestFocus();

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
