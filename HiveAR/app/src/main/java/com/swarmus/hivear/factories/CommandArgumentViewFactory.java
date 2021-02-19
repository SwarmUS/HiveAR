package com.swarmus.hivear.factories;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.swarmus.hivear.FunctionCall;
import com.swarmus.hivear.R;
import com.swarmus.hivear.enums.CommandType;

import java.util.Arrays;
import java.util.List;

public class CommandArgumentViewFactory {
    private CommandArgumentViewFactory() {} // make Factory act as a static class

    public static List<View> createCommandArgumentViews(LayoutInflater inflater, CommandType commandType) {
        switch (commandType) {
            case moveBy:
                return getMoveByArgumentViews(inflater);
            case doABackFlip:
                return getDoABackFlipArgumentViews(inflater);
        }
        return null;
    }

    public static List<View> createCommandArgumentViews(LayoutInflater inflater, FunctionCall.FunctionCallRequest functionCallRequest) {

        for (FunctionCall.FunctionArgument arg : functionCallRequest.getArgumentsList()) {
            // Get argument name
            //
            // Get argument type
        }
        return null;
    }

    private static List<View> getMoveByArgumentViews(LayoutInflater inflater) {

        View argX = inflater.inflate(R.layout.command_argument, null, false);
        TextView argXName = argX.findViewById(R.id.command_argument_name);
        argXName.setText("X offset");
        EditText argXInput = argX.findViewById(R.id.command_argument_value);
        argXInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        View argY = inflater.inflate(R.layout.command_argument, null, false);
        TextView argYName = argY.findViewById(R.id.command_argument_name);
        argYName.setText("Y offset");
        EditText argYInput = argY.findViewById(R.id.command_argument_value);
        argYInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        return Arrays.asList(argX, argY);
    }

    private static List<View> getDoABackFlipArgumentViews(LayoutInflater inflater) {

        View arg1 = inflater.inflate(R.layout.command_argument, null, false);
        TextView argName1 = arg1.findViewById(R.id.command_argument_name);
        argName1.setText("Arg 1 Signed");
        EditText argInput1 = arg1.findViewById(R.id.command_argument_value);
        argInput1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

        View arg2 = inflater.inflate(R.layout.command_argument, null, false);
        TextView argName2 = arg2.findViewById(R.id.command_argument_name);
        argName2.setText("Arg 2 datetime");
        EditText argInput2 = arg2.findViewById(R.id.command_argument_value);
        argInput2.setInputType(InputType.TYPE_CLASS_DATETIME);

        View arg3 = inflater.inflate(R.layout.command_argument, null, false);
        TextView argName3 = arg3.findViewById(R.id.command_argument_name);
        argName3.setText("Arg 3 text");
        EditText argInput3 = arg3.findViewById(R.id.command_argument_value);
        argInput3.setInputType(InputType.TYPE_CLASS_TEXT);

        View arg4 = inflater.inflate(R.layout.command_argument, null, false);
        TextView argName4 = arg4.findViewById(R.id.command_argument_name);
        argName4.setText("Arg 4");

        return Arrays.asList(arg1, arg2, arg3, arg4);
    }
}
