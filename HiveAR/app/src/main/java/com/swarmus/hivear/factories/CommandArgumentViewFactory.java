package com.swarmus.hivear.factories;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.swarmus.hivear.R;
import com.swarmus.hivear.models.ProtoFunctionCallTemplate;

import java.util.ArrayList;
import java.util.List;

public class CommandArgumentViewFactory {
    private CommandArgumentViewFactory() {} // make Factory act as a static class

    public static List<View> createCommandArgumentViews(View view, List<ProtoFunctionCallTemplate.Argument> arguments) {
        List<View> argViews = new ArrayList<>();

        int i = 0;
        for (ProtoFunctionCallTemplate.Argument arg : arguments) {
            View argView = View.inflate(view.getContext(), R.layout.command_argument, null);

            // We should retrieve argument name in the future.
            TextView argName = argView.findViewById(R.id.command_argument_name);
            argName.setText(arg.getName());

            EditText argInput = argView.findViewById(R.id.command_argument_value);

            if (arg.getValue() instanceof Integer) {
                argInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else if (arg.getValue() instanceof Float) {
                argInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            } else {
                continue;
            }
            argViews.add(argView);
            i++;
        }

        return argViews;
    }
}
