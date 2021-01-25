package com.swarmus.hivear.activities;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swarmus.hivear.R;

import androidx.appcompat.app.AppCompatActivity;

public class ARActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_view);

        FloatingActionButton toCommandButton = findViewById(R.id.toCommandButton);
        toCommandButton.setOnClickListener(view -> {
            Intent goToCommand = new Intent(getApplicationContext(), MainActivity.class);
            goToCommand.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(goToCommand);
        });
    }
}