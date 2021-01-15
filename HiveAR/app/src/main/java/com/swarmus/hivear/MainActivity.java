package com.swarmus.hivear;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton toARButton = findViewById(R.id.toARButton);
        toARButton.setOnClickListener(view -> {
            Intent goToAR = new Intent(getApplicationContext(), ARActivity.class);
            goToAR.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            goToAR.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivity(goToAR);
        });

        FloatingActionButton toDevicesButton = findViewById(R.id.toDevicesButton);
        toDevicesButton.setOnClickListener(view -> {
            Intent gotoDevices = new Intent(getApplicationContext(), DeviceActivity.class);
            gotoDevices.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            gotoDevices.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivity(gotoDevices);
        });
    }
}