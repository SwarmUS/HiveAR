package com.swarmus.hivear.factories;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.swarmus.hivear.FunctionCall;
import com.swarmus.hivear.R;

import java.util.ArrayList;
import java.util.List;

public class CommandArgumentViewFactory {
    private CommandArgumentViewFactory() {} // make Factory act as a static class

    public static List<View> createCommandArgumentViews(View view, FunctionCall.FunctionCallRequest functionCallRequest) {
        List<View> argViews = new ArrayList<>();

        int i = 0;
        for (FunctionCall.FunctionArgument arg : functionCallRequest.getArgumentsList()) {
            View argView = LayoutInflater.from(view.getContext()).inflate(R.layout.command_argument, null, false);

            // We should retrieve argument name in the future.
            TextView argName = argView.findViewById(R.id.command_argument_name);
            argName.setText("Arg " + Integer.toString(i));

            EditText argInput = argView.findViewById(R.id.command_argument_value);
            switch (arg.getArgumentCase()) {
                case INT_ARG:
                    argInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;
                case FLOAT_ARG:
                    argInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    break;
                case ARGUMENT_NOT_SET:
                    continue;
            }
            argViews.add(argView);
            i++;
        }

        return argViews;
    }
}
