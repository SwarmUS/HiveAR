package com.swarmus.hivear.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.R;
import com.swarmus.hivear.commands.GenericCommand;
import com.swarmus.hivear.enums.ConnectionStatus;
import com.swarmus.hivear.fragments.ConnectionViewFragment;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.SerialSettingsViewModel;
import com.swarmus.hivear.models.TCPDevice;
import com.swarmus.hivear.models.TcpSettingsViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    NavHostFragment navHostFragment;

    private SerialDevice serialDevice;
    private TCPDevice tcpDevice;
    private CommunicationDevice currentCommunicationDevice;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEFAULT_IP_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 3000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout);

        setUpNavigation();
        setUpCommmunication();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_UP) {
            final View view = getCurrentFocus();

            if(view != null) {
                final boolean consumed = super.dispatchTouchEvent(ev);

                final View viewTmp = getCurrentFocus();
                final View viewNew = viewTmp != null ? viewTmp : view;

                if(viewNew.equals(view)) {
                    final Rect rect = new Rect();
                    final int[] coordinates = new int[2];

                    view.getLocationOnScreen(coordinates);

                    rect.set(coordinates[0], coordinates[1], coordinates[0] + view.getWidth(), coordinates[1] + view.getHeight());

                    final int x = (int) ev.getX();
                    final int y = (int) ev.getY();

                    if(rect.contains(x, y)) {
                        return consumed;
                    }
                }
                else if(viewNew instanceof EditText) {
                    return consumed;
                }

                final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(viewNew.getWindowToken(), 0);
                viewNew.clearFocus();
                return consumed;
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    public CommunicationDevice getCurrentCommunicationDevice() {return currentCommunicationDevice;}

    public CommunicationDevice switchCurrentCommunicationDevice() {
        currentCommunicationDevice.endConnection();
        currentCommunicationDevice.setActive(false);
        if (currentCommunicationDevice instanceof SerialDevice) {
            currentCommunicationDevice = tcpDevice;
        } else if (currentCommunicationDevice instanceof TCPDevice) {
            currentCommunicationDevice = serialDevice;
        }
        currentCommunicationDevice.setActive(true);
        return currentCommunicationDevice;
    }

    public void sendCommand(@NonNull GenericCommand command) {
        currentCommunicationDevice.sendData(command.getCommand());
    }

    private void setUpNavigation() {
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.getNavController());
        setConnectionBadge(ConnectionStatus.notConnected);
    }

    private void setConnectionBadge(ConnectionStatus status) {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.connectionViewFragment);
        if (badgeDrawable != null) {
            badgeDrawable.setVisible(true);
            switch (status) {
                case connected:
                    badgeDrawable.setBackgroundColor(getColor(R.color.connection_established));
                    break;
                case notConnected:
                    badgeDrawable.setBackgroundColor(getColor(R.color.connection_none));
                    break;
                case connecting:
                    badgeDrawable.setBackgroundColor(getColor(R.color.connection_pending));
                    break;
            }
        }
    }

    private void setUpCommmunication() {
        IntentFilter filterConnectionStatus = new IntentFilter(CommunicationDevice.CONNECTION_STATUS_RESULT);
        registerReceiver(deviceConnectionStatusReceiver, filterConnectionStatus);

        setUpTCPCommunication();
        setUpSerialCommunication();

        currentCommunicationDevice = serialDevice;
    }

    private void setUpTCPCommunication() {
        tcpDevice = new TCPDevice(DEFAULT_IP_ADDRESS, DEFAULT_PORT);
        tcpDevice.init(this, connectionCallback);

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> tcpDevice.setServerIP(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);

        final Observer<Integer> portObserver = p -> tcpDevice.setServerPort(p);
        tcpSettingsViewModel.getPort().observe(this, portObserver);
    }

    private void setUpSerialCommunication() {
        serialDevice = new SerialDevice();
        serialDevice.init(this, connectionCallback);

        IntentFilter filter = new IntentFilter(SerialDevice.ACTION_SERIAL_DEVICE_CHANGED);
        registerReceiver(serialDeviceChangedReceiver, filter);

        SerialSettingsViewModel serialSettingsViewModel = new ViewModelProvider(this).get(SerialSettingsViewModel.class);
        serialSettingsViewModel.getSelectedDevice().observe(this, deviceName -> serialDevice.setSelectedUsbDeviceName(deviceName));
    }

    private final BroadcastReceiver serialDeviceChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SerialDevice.ACTION_SERIAL_DEVICE_CHANGED.equals(action)) {
                updateSerialDevices((HashMap<String, String>)intent.getSerializableExtra(SerialDevice.EXTRA_SERIAL_DEVICE_CHANGED));
            }
        }
    };

    private void updateSerialDevices(HashMap<String, String> deviceMap) {
        SerialSettingsViewModel serialSettingsViewModel = new ViewModelProvider(this).get(SerialSettingsViewModel.class);
        serialSettingsViewModel.getDevices().setValue(deviceMap);
    }

    private final BroadcastReceiver deviceConnectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CommunicationDevice.CONNECTION_STATUS_RESULT.equals(action)) {
                ConnectionStatus connectionStatus = (ConnectionStatus) intent.getExtras().get(CommunicationDevice.EXTRA_CONNECTION_STATUS_RESULT);
                setConnectionBadge(connectionStatus);
            }
        }
    };

    final CommunicationDevice.ConnectionCallback connectionCallback = new CommunicationDevice.ConnectionCallback() {
        @Override
        public void onConnect() {
            Log.d(TAG, "New Connection");
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.connected);
            InputStream inputStream = currentCommunicationDevice.getDataStream();
            Thread thread = new Thread(() -> {
                while (inputStream != null) {
                    try {
                        MessageOuterClass.Message receivedMessage = MessageOuterClass.Message.parseDelimitedFrom(inputStream);
                        logProtoMessage(receivedMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            });
            thread.start();
        }

        private void logProtoMessage(MessageOuterClass.Message msg)
        {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof ConnectionViewFragment && msg != null) {
                TextView dataReceived = findViewById(R.id.dataReceived);
                appendTextAndScroll(dataReceived, "\n");
                appendTextAndScroll(dataReceived, "Destination_id: " + msg.getDestinationId() + "\n");
                appendTextAndScroll(dataReceived, "Source_id: " + msg.getSourceId() + "\n");
                if (msg.hasRequest()) {appendTextAndScroll(dataReceived, "Request: " + msg.getRequest() + "\n");}
                else if (msg.hasResponse()) {appendTextAndScroll(dataReceived, "Response: " + msg.getResponse() + "\n");}
                appendTextAndScroll(dataReceived, "\n");
            }
            else if (msg != null){
                Log.d(TAG, "Proto Message:");
                Log.d(TAG, "DestinationId: " + msg.getDestinationId());
                Log.d(TAG, "SourceID: " + msg.getSourceId());
                if (msg.hasRequest()) {Log.d(TAG, "Request: " + msg.getRequest());}
                else {Log.d(TAG, "Response: " + msg.getResponse());}
            }
        }

        @Override
        public void onDisconnect() {
            Log.d(TAG, "End of Connection");
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
        }

        @Override
        public void onConnectError() {
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
        }
    };

    private void appendTextAndScroll(TextView tv, String text)
    {
        if(tv != null){
            tv.append(text);
            final Layout layout = tv.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(tv.getLineCount() - 1)
                        - tv.getScrollY() - tv.getHeight();
                if(scrollDelta > 0)
                    tv.scrollBy(0, scrollDelta);
            }
        }
    }
}