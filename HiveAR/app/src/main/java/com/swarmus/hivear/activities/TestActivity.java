package com.swarmus.hivear.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialInterface;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.commands.MoveByCommand;
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

    private MoveByCommand upCommand;
    private MoveByCommand downCommand;
    private MoveByCommand leftCommand;
    private MoveByCommand rightCommand;
    private MoveByCommand stopCommand;

    private MessageOuterClass.Message receivedMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        serialSettingsViewModel = new ViewModelProvider(this).get(SerialSettingsViewModel.class);

        tcpSettingsFrag = new TcpSettingsFragment();
        uartSettingsFrag = new UartSettingsFragment();

        // TODO CODE FOR ConnectionViewFragment
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
        serialDevice.init(this, connectionCallback);

        tcpDevice = new TCPDevice(DEFAULT_IP_ADDRESS, DEFAULT_PORT);
        tcpDevice.init(this, connectionCallback);

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> ((TCPDevice)tcpDevice).setServerIP(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);

        final Observer<Integer> portObserver = p -> ((TCPDevice)tcpDevice).setServerPort(p);
        tcpSettingsViewModel.getPort().observe(this, portObserver);

        upCommand = new MoveByCommand(1,0);
        findViewById(R.id.upButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null)
            {
                currentCommunicationDevice.sendData(upCommand.getCommand());
            }
        });
        downCommand = new MoveByCommand(-1,0);
        findViewById(R.id.downButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null)
            {
                currentCommunicationDevice.sendData(downCommand.getCommand());
            }
        });
        leftCommand = new MoveByCommand(0,1);
        findViewById(R.id.leftButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null)
            {
                currentCommunicationDevice.sendData(leftCommand.getCommand());
            }
        });
        rightCommand = new MoveByCommand(0,-1);
        findViewById(R.id.rightButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null)
            {
                currentCommunicationDevice.sendData(rightCommand.getCommand());
            }
        });
        stopCommand = new MoveByCommand(0,0);
        findViewById(R.id.stopButton).setOnClickListener(view -> {
            if (currentCommunicationDevice != null)
            {
                currentCommunicationDevice.sendData(stopCommand.getCommand());
            }
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

    final CommunicationDevice.ConnectionCallback connectionCallback = new CommunicationDevice.ConnectionCallback() {
        @Override
        public void onConnect() {
            Log.d(TAG, "New Connection");
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.connected);
            InputStream inputStream = currentCommunicationDevice.getDataStream();
            Thread thread = new Thread(() -> {
                while (inputStream != null) {
                    try {
                        receivedMessage = MessageOuterClass.Message.parseDelimitedFrom(inputStream);
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
            if (msg != null && dataReceived != null)
            {
                appendTextAndScroll(dataReceived, "\n");
                appendTextAndScroll(dataReceived, "Destination_id: " + msg.getDestinationId() + "\n");
                appendTextAndScroll(dataReceived, "Source_id: " + msg.getSourceId() + "\n");
                if (msg.hasRequest()) {appendTextAndScroll(dataReceived, "Request: " + msg.getRequest() + "\n");}
                else if (msg.hasResponse()) {appendTextAndScroll(dataReceived, "Response: " + msg.getResponse() + "\n");}
                appendTextAndScroll(dataReceived, "\n");
            }
        }

        @Override
        public void onDisconnect() {
            Log.d(TAG, "End of Connection");
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
            if (receivedMessage != null) {receivedMessage.toBuilder().clear();}
        }

        @Override
        public void onConnectError() {
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
            if (receivedMessage != null) {receivedMessage.toBuilder().clear();}
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