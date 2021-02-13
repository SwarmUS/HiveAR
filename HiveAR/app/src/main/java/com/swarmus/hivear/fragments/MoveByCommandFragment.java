package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.swarmus.hivear.R;

public class MoveByCommandFragment extends Fragment {

    // TODO Set as argument to call back to send the MoveBy command
    public MoveByCommandFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_moveby_command, container, false);

        v.findViewById(R.id.send_command_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // TODO construct command
                // TODO send command
                return false;
            }
        });

        return v;
    }

}
