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
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialInterface;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.TCPDevice;
import com.swarmus.hivear.fragments.TcpSettingsFragment;
import com.swarmus.hivear.fragments.UartSettingsFragment;
import com.swarmus.hivear.models.TcpSettingsViewModel;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

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

        dataReceived = findViewById(R.id.dataReceived);
        dataReceived.setMovementMethod(new ScrollingMovementMethod());

        FloatingActionButton toCommandButton = findViewById(R.id.toCommandButton2);
        toCommandButton.setOnClickListener(view -> {
            Intent goToCommand = new Intent(getApplicationContext(), MainActivity.class);
            goToCommand.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(goToCommand);
        });

        currentCommunicationDevice = serialDevice = new SerialDevice(this);
        serialDevice.init();
        IntentFilter filter = new IntentFilter(SerialDevice.ACTION_SERIAL_DEVICE_CHANGED);
        registerReceiver(serialDeviceChangedReceiver, filter);

        tcpDevice = new TCPDevice(DEFAULT_IP_ADDRESS, DEFAULT_PORT);
        tcpDevice.init();

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> ((TCPDevice)tcpDevice).setIp(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);

        final Observer<Integer> portObserver = p -> ((TCPDevice)tcpDevice).setPort(p);
        tcpSettingsViewModel.getPort().observe(this, portObserver);

        switchCommunication(uartSettingsFrag);
    }

    private void setSerialDeviceName(String deviceName) {
        if (uartSettingsFrag != null) {
            uartSettingsFrag.setDeviceName(deviceName);
        }
    }

    private void switchCommunication(Fragment toShow) {
        if (toShow instanceof TcpSettingsFragment) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.hide(uartSettingsFrag);
            ft.show(toShow);
            ft.commit();
            currentCommunicationDevice.endConnection();
            serialDevice.removeReadCallBack();
            ((TCPDevice)tcpDevice).setClientCallback(tcpCallBack);
            currentCommunicationDevice = tcpDevice;
            currentCommunicationDevice.establishConnection();
        } else if (toShow instanceof UartSettingsFragment) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.hide(tcpSettingsFrag);
            ft.show(toShow);
            ft.commit();
            currentCommunicationDevice.endConnection();
            tcpDevice.removeReadCallBack();
            ((SerialDevice)serialDevice).setReadCB(usbReadCallback);
            currentCommunicationDevice = serialDevice;
            currentCommunicationDevice.establishConnection();
        }
        dataReceived.setText("");
    }

    final TCPDevice.ClientCallback tcpCallBack = new TCPDevice.ClientCallback() {
        @Override
        public void onMessage(String message) {
            if (dataReceived != null) {
                appendTextAndScroll(dataReceived, message+"\n");
            }
        }

        @Override
        public void onConnect(Socket socket) {
            // TODO Show status somewhere
            Log.d(TAG, "New TCP Connection");
        }

        @Override
        public void onDisconnect(Socket socket, String message) {
            // TODO Show status somewhere
            Log.d(TAG, "End of TCP Connection");
        }

        @Override
        public void onConnectError(Socket socket, String message) {
            // TODO Show status somewhere
        }
    };

    final UsbSerialInterface.UsbReadCallback usbReadCallback = (data) -> {
        // TODO future messages will have a different data structure than String
        String dataStr = new String(data, StandardCharsets.UTF_8);
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

    public final BroadcastReceiver serialDeviceChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SerialDevice.ACTION_SERIAL_DEVICE_CHANGED.equals(action)) {
                setSerialDeviceName(intent.getStringExtra(SerialDevice.EXTRA_SERIAL_DEVICE_CHANGED));
            }
        }
    };
}