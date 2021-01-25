package com.swarmus.hivear;

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

import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialInterface;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestActivity extends AppCompatActivity {
    private TextView dataReceived;
    private CommunicationDevice serialDevice;
    private CommunicationDevice tcpDevice;
    private CommunicationDevice currentCommunicationDevice;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEFAULT_IP_ADDRESS = "192.168.0.26";
    private static final int DEFAULT_PORT = 3000;

    private TcpSettings tcpSettingsFrag;
    private UartSettings uartSettingsFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        tcpSettingsFrag = new TcpSettings();
        uartSettingsFrag = new UartSettings();

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
                view.invalidate();
            } else {
                switchCommunication(uartSettingsFrag);
                switchCommunicationButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.usb_icon));
                view.invalidate();
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

        currentCommunicationDevice = serialDevice = new SerialDevice(this);
        serialDevice.init();
        IntentFilter filter = new IntentFilter(SerialDevice.ACTION_SERIAL_DEVICE_CHANGED);
        registerReceiver(serialDeviceChangedReceiver, filter);

        tcpDevice = new TCPDevice(DEFAULT_IP_ADDRESS, DEFAULT_PORT);
        tcpDevice.init();

        switchCommunication(uartSettingsFrag);
    }

    private void setSerialDeviceName(String deviceName) {
        if (uartSettingsFrag != null) {
            /*Bundle bundle = new Bundle();
            bundle.putString("serialDeviceName", deviceName);
            uartSettingsFrag.setArguments(bundle);*/
            uartSettingsFrag.setDeviceName(deviceName);
        }
    }

    private void switchCommunication(Fragment toShow) {
        if (toShow instanceof TcpSettings) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            //ft.replace(R.id.communicationContainer, toShow);
            ft.hide(uartSettingsFrag);
            ft.show(toShow);
            ft.commit();
            currentCommunicationDevice.endConnection();
            ((TCPDevice)tcpDevice).setClientCallback(tcpCallBack);
            currentCommunicationDevice = tcpDevice;
            currentCommunicationDevice.establishConnection();
        } else if (toShow instanceof UartSettings) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            //ft.replace(R.id.communicationContainer, toShow);
            ft.hide(tcpSettingsFrag);
            ft.show(toShow);
            ft.commit();
            currentCommunicationDevice.endConnection();
            ((SerialDevice)serialDevice).setReadCB(usbReadCallback);
            currentCommunicationDevice = serialDevice;
            currentCommunicationDevice.establishConnection();
        }
        dataReceived.setText("");
    }

    TCPDevice.ClientCallback tcpCallBack = new TCPDevice.ClientCallback() {
        @Override
        public void onMessage(String message) {
            if (dataReceived != null) {
                appendTextAndScroll(dataReceived, message+"\n");
            }
        }

        @Override
        public void onConnect(Socket socket) {
            // Show status somewhere
            Log.d(TAG, "New TCP Connection");
        }

        @Override
        public void onDisconnect(Socket socket, String message) {
            // Show status somewhere
        }

        @Override
        public void onConnectError(Socket socket, String message) {
            // Show status somewhere
        }
    };

    UsbSerialInterface.UsbReadCallback usbReadCallback = (data) -> {
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