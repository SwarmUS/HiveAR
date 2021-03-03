package com.swarmus.hivear.factories;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.R;

import java.util.ArrayList;
import java.util.List;

public class CommandArgumentViewFactory {
    private CommandArgumentViewFactory() {} // make Factory act as a static class

    public static List<View> createCommandArgumentViews(View view, MessageOuterClass.FunctionCallRequest functionCallRequest) {
        List<View> argViews = new ArrayList<>();

        int i = 0;
        for (MessageOuterClass.FunctionArgument arg : functionCallRequest.getArgumentsList()) {
            View argView = View.inflate(view.getContext(), R.layout.command_argument, null);

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
