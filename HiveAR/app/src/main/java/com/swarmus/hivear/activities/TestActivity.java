package com.swarmus.hivear.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialInterface;
import com.swarmus.hivear.enums.ConnectionStatus;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.SerialSettingsViewModel;
import com.swarmus.hivear.models.TCPDevice;
import com.swarmus.hivear.fragments.TcpSettingsFragment;
import com.swarmus.hivear.fragments.UartSettingsFragment;
import com.swarmus.hivear.models.TcpSettingsViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TestActivity extends AppCompatActivity {
    private TextView dataReceived;
    private CommunicationDevice serialDevice;
    private CommunicationDevice tcpDevice;
    private CommunicationDevice currentCommunicationDevice;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEFAULT_IP_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 3000;

    private TcpSettingsFragment tcpSettingsFrag;
    private UartSettingsFragment uartSettingsFrag;

    private SerialSettingsViewModel serialSettingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        serialSettingsViewModel = new ViewModelProvider(this).get(SerialSettingsViewModel.class);

        tcpSettingsFrag = new TcpSettingsFragment();
        uartSettingsFrag = new UartSettingsFragment();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.communicationContainer, tcpSettingsFrag);
        ft.add(R.id.communicationContainer, uartSettingsFrag);
        ft.commit();

        FloatingActionButton switchCommunicationButton = findViewById(R.id.switchCommunication);
        switchCommunicationButton.setOnClickListener(view -> {
            if (currentCommunicationDevice instanceof SerialDevice) {
                switchCommunication(tcpSettingsFrag);
                switchCommunicationButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.wifi_icon));
            } else {
                switchCommunication(uartSettingsFrag);
                switchCommunicationButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.usb_icon));
            }
            view.invalidate();
        });

        IntentFilter filterConnectionStatus = new IntentFilter(CommunicationDevice.CONNECTION_STATUS_RESULT);
        registerReceiver(deviceConnectionStatusReceiver, filterConnectionStatus);

        serialSettingsViewModel.getSelectedDevice().observe(this, deviceName -> ((SerialDevice)serialDevice).setSelectedUsbDeviceName(deviceName));

        findViewById(R.id.connectButton).setOnClickListener(view -> {
            if(currentCommunicationDevice!=null){
                currentCommunicationDevice.establishConnection();
            }
        });
        findViewById(R.id.disconnectButton).setOnClickListener(view -> {
            if(currentCommunicationDevice!=null){
                currentCommunicationDevice.endConnection();
            }
        });

        dataReceived = findViewById(R.id.dataReceived);
        dataReceived.setMovementMethod(new ScrollingMovementMethod());

        FloatingActionButton toCommandButton = findViewById(R.id.toCommandButton2);
        toCommandButton.setOnClickListener(view -> {
            Intent goToCommand = new Intent(getApplicationContext(), MainActivity.class);
            goToCommand.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(goToCommand);
        });

        IntentFilter filter = new IntentFilter(SerialDevice.ACTION_SERIAL_DEVICE_CHANGED);
        registerReceiver(serialDeviceChangedReceiver, filter);

        currentCommunicationDevice = serialDevice = new SerialDevice();
        serialDevice.init(this);

        tcpDevice = new TCPDevice(DEFAULT_IP_ADDRESS, DEFAULT_PORT, tcpCallBack);
        tcpDevice.init(this);

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> ((TCPDevice)tcpDevice).setServerIP(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);

        final Observer<Integer> portObserver = p -> ((TCPDevice)tcpDevice).setServerPort(p);
        tcpSettingsViewModel.getPort().observe(this, portObserver);

        findViewById(R.id.upButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null) { currentCommunicationDevice.sendData("UP");}
        });
        findViewById(R.id.downButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null) { currentCommunicationDevice.sendData("DOWN");}
        });
        findViewById(R.id.leftButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null) { currentCommunicationDevice.sendData("LEFT");}
        });
        findViewById(R.id.rightButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null) { currentCommunicationDevice.sendData("RIGHT");}
        });
        findViewById(R.id.stopButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null) { currentCommunicationDevice.sendData("STOP");}
        });

        switchCommunication(uartSettingsFrag);
    }

    private void switchCommunication(Fragment toShow) {
        if (toShow instanceof TcpSettingsFragment) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.hide(uartSettingsFrag);
            ft.show(toShow);
            ft.commit();
            currentCommunicationDevice.endConnection();
            serialDevice.setActive(false);
            currentCommunicationDevice = tcpDevice;
            tcpDevice.setActive(true);
        } else if (toShow instanceof UartSettingsFragment) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.hide(tcpSettingsFrag);
            ft.show(toShow);
            ft.commit();
            currentCommunicationDevice.endConnection();
            tcpDevice.setActive(false);
            currentCommunicationDevice = serialDevice;
            serialDevice.setActive(true);
        }
        ((ImageView)findViewById(R.id.connectionStatusImage)).setColorFilter(getColor(R.color.connection_none));
        dataReceived.setText("");
    }

    final TCPDevice.ClientCallback tcpCallBack = new TCPDevice.ClientCallback() {
        @Override
        public void onConnect() {
            Log.d(TAG, "New TCP Connection");
            tcpDevice.broadCastConnectionStatus(ConnectionStatus.connected);
        }

        @Override
        public void onDisconnect() {
            Log.d(TAG, "End of TCP Connection");
            tcpDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
        }

        @Override
        public void onConnectError() {
            tcpDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
        }
    };

    final UsbSerialInterface.UsbReadCallback usbReadCallback = (data) -> {
        InputStream stream = serialDevice.getDataStream();
        // TODO future messages will have a different data structure than String
        byte[] dataFromStream = new byte[data.length];
        try {
            stream.read(dataFromStream, 0, data.length);
        } catch (IOException e) {
            // TODO: Handle
            e.printStackTrace();
        }
        String dataStr = new String(dataFromStream, StandardCharsets.UTF_8);
        dataStr += "\n";
        if (dataReceived != null) {
            appendTextAndScroll(dataReceived, dataStr);
        }
        Log.i(TAG, "Data received: " + dataStr);
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

    private final BroadcastReceiver serialDeviceChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SerialDevice.ACTION_SERIAL_DEVICE_CHANGED.equals(action)) {
                if (serialSettingsViewModel != null) {
                    serialSettingsViewModel.getDevices().setValue((HashMap<String, String>)intent.getSerializableExtra(SerialDevice.EXTRA_SERIAL_DEVICE_CHANGED));
                }
            }
        }
    };

    private final BroadcastReceiver deviceConnectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CommunicationDevice.CONNECTION_STATUS_RESULT.equals(action)) {
                ConnectionStatus connectionStatus = (ConnectionStatus) intent.getExtras().get(CommunicationDevice.EXTRA_CONNECTION_STATUS_RESULT);
                switch (connectionStatus) {
                    case connected:
                        ((ImageView)findViewById(R.id.connectionStatusImage)).setColorFilter(getColor(R.color.connection_established));
                        break;
                    case notConnected:
                        ((ImageView)findViewById(R.id.connectionStatusImage)).setColorFilter(getColor(R.color.connection_none));
                        break;
                    case connecting:
                        ((ImageView)findViewById(R.id.connectionStatusImage)).setColorFilter(getColor(R.color.connection_pending));
                        break;
                }
            }
        }
    };
}