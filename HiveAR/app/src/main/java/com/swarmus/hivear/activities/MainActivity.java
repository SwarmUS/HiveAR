package com.swarmus.hivear.activities;

import android.content.Intent;
import android.os.Bundle;
import android.service.quickaccesswallet.SelectWalletCardRequest;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationMenu;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.internal.NavigationMenuView;
import com.swarmus.hivear.R;
import com.swarmus.hivear.enums.ConnectionStatus;
import com.swarmus.hivear.fragments.ARViewFragment;
import com.swarmus.hivear.fragments.ConnectionViewFragment;
import com.swarmus.hivear.fragments.RobotDetailsViewFragment;
import com.swarmus.hivear.fragments.SwarmSummaryViewFragment;
import com.swarmus.hivear.fragments.TcpSettingsFragment;
import com.swarmus.hivear.fragments.UartSettingsFragment;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.SerialSettingsViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavHost;
import androidx.navigation.NavHostController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    private CommunicationDevice serialDevice;
    private CommunicationDevice tcpDevice;
    private CommunicationDevice currentCommunicationDevice;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEFAULT_IP_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 3000;

    /*@Override
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
            Intent gotoDevices = new Intent(getApplicationContext(), TestActivity.class);
            gotoDevices.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            gotoDevices.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivity(gotoDevices);
        });
    }*/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout);

        setUpNavigation();
        setUpCommmunication();
    }

    private void setUpNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.getNavController());
        setConnectionBadge(ConnectionStatus.notConnected);
    }

    private void setConnectionBadge(ConnectionStatus status) {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.connectionViewFragment);
        if (badgeDrawable != null) {
            badgeDrawable.setVisible(true);
            if (status == ConnectionStatus.connected) {
                badgeDrawable.setBackgroundColor(getColor(R.color.connection_established));
            } else if (status == ConnectionStatus.connecting) {
                badgeDrawable.setBackgroundColor(getColor(R.color.connection_pending));
            } else if (status == ConnectionStatus.notConnected) {
                badgeDrawable.setBackgroundColor(getColor(R.color.connection_none));
            }
        }
    }

    private void setUpCommmunication() {

    }
}