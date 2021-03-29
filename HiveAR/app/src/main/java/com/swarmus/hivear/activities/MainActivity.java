package com.swarmus.hivear.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.R;
import com.swarmus.hivear.arcore.CameraPermissionHelper;
import com.swarmus.hivear.commands.GenericCommand;
import com.swarmus.hivear.enums.ConnectionStatus;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateArgument;
import com.swarmus.hivear.models.ProtoMsgViewModel;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.models.RobotListViewModel;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.SerialSettingsViewModel;
import com.swarmus.hivear.models.SettingsViewModel;
import com.swarmus.hivear.models.SwarmAgentInfoViewModel;
import com.swarmus.hivear.models.TCPDevice;
import com.swarmus.hivear.models.TcpSettingsViewModel;
import com.swarmus.hivear.utils.ProtoMsgStorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    NavHostFragment navHostFragment;

    private CommunicationDevice serialDevice;
    private CommunicationDevice tcpDevice;
    private CommunicationDevice currentCommunicationDevice;

    private SwarmAgentInfoViewModel swarmAgentInfoViewModel;
    private static final String BROADCAST_PROTO_MSG_RECEIVED = "Proto Message Received";
    private ProtoMsgStorer protoMsgStorer;
    private Queue<MessageOuterClass.Message> receivedMessages;

    private RobotListViewModel robotListViewModel;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEFAULT_IP_ADDRESS = "192.168.0.";
    private static final int DEFAULT_PORT = 12345;

    private boolean userRequestedInstall = true;
    private boolean arEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout);

        setupSettings();
        maybeEnableAr(); // Hide AR tab if not possible to do AR
        setUpNavigation();
        setUpCommmunication();
        robotListViewModel = new ViewModelProvider(this).get(RobotListViewModel.class);
        updateRobots();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!arEnabled) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !userRequestedInstall)) {
                    case INSTALL_REQUESTED:
                        userRequestedInstall = false;
                        return;
                    case INSTALLED:
                        break;
                }

                arEnabled = true;
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG)
                        .show();
                Log.e("ARCore", "Exception creating session", exception);
                return;
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
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

    private void setupSettings() {
        SettingsViewModel settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        settingsViewModel.updateDatabaseDirs(this);
        List<String> allDatabases = settingsViewModel.getAllDatabases().getValue();
        if (allDatabases != null && !allDatabases.isEmpty()) {
            // Set first element as default folder
            settingsViewModel.getActiveDatabaseFolder().setValue(allDatabases.get(0));
        }
    }

    private void maybeEnableAr() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(this::maybeEnableAr, 200);
        }
        if (!availability.isSupported()) {
            bottomNavigationView = findViewById(R.id.bottom_nav_view);
            // Show or not the AR view
            bottomNavigationView.getMenu().removeItem(R.id.ARViewFragment);
        }
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
        swarmAgentInfoViewModel = new ViewModelProvider(this).get(SwarmAgentInfoViewModel.class);

        receivedMessages = new LinkedList<>();
        protoMsgStorer = new ProtoMsgStorer(6);
        ProtoMsgViewModel protoMsgViewModel = new ViewModelProvider(this).get(ProtoMsgViewModel.class);
        protoMsgViewModel.getProtoMessages().observe(this, s -> Log.i(TAG, protoMsgViewModel.getProtoMessages().getValue()));

        IntentFilter filterProtoMsgReceived = new IntentFilter(BROADCAST_PROTO_MSG_RECEIVED);
        registerReceiver(protoMsgReadReceiver, filterProtoMsgReceived);

        IntentFilter filterConnectionStatus = new IntentFilter(CommunicationDevice.CONNECTION_STATUS_RESULT);
        registerReceiver(deviceConnectionStatusReceiver, filterConnectionStatus);

        setUpTCPCommunication();
        setUpSerialCommunication();

        currentCommunicationDevice = serialDevice;
        currentCommunicationDevice.setActive(true);
    }

    private void setUpTCPCommunication() {
        tcpDevice = new TCPDevice(DEFAULT_IP_ADDRESS, DEFAULT_PORT);
        tcpDevice.init(this, connectionCallback);

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> ((TCPDevice)tcpDevice).setServerIP(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);

        final Observer<Integer> portObserver = p -> ((TCPDevice)tcpDevice).setServerPort(p);
        tcpSettingsViewModel.getPort().observe(this, portObserver);
    }

    private void setUpSerialCommunication() {
        serialDevice = new SerialDevice();

        IntentFilter filter = new IntentFilter(SerialDevice.ACTION_SERIAL_DEVICE_CHANGED);
        registerReceiver(serialDeviceChangedReceiver, filter);

        SerialSettingsViewModel serialSettingsViewModel = new ViewModelProvider(this).get(SerialSettingsViewModel.class);
        serialSettingsViewModel.getSelectedDevice().observe(this, deviceName -> ((SerialDevice)serialDevice).setSelectedUsbDeviceName(deviceName));

        // Must be after receiver
        serialDevice.init(this, connectionCallback);
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
        MutableLiveData<HashMap<String, String>> devices = serialSettingsViewModel.getDevices();
        devices.setValue(deviceMap);
        if (!devices.getValue().isEmpty()) {
            serialSettingsViewModel.getSelectedDevice().setValue(devices.getValue().values().iterator().next());
        }
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
            Intent msgReceivedIntent = new Intent();
            msgReceivedIntent.setAction(BROADCAST_PROTO_MSG_RECEIVED);
            Thread thread = new Thread(() -> {
                while (inputStream != null) {
                    try {
                        MessageOuterClass.Message msg = MessageOuterClass.Message.parseDelimitedFrom(inputStream);
                        receivedMessages.add(msg);
                        sendBroadcast(msgReceivedIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            });
            thread.start();

            // Send greet to get a swarm agent ID
            sendGreet();
        }

        @Override
        public void onDisconnect() {
            Log.d(TAG, "End of Connection");
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
            swarmAgentInfoViewModel.getSwarmAgentID().setValue(SwarmAgentInfoViewModel.DEFAULT_SWARM_AGENT_ID);
        }

        @Override
        public void onConnectError() {
            currentCommunicationDevice.broadCastConnectionStatus(ConnectionStatus.notConnected);
        }
    };

    private final BroadcastReceiver protoMsgReadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BROADCAST_PROTO_MSG_RECEIVED.equals(action)) {
                MessageOuterClass.Message msg;
                while ((msg = receivedMessages.poll()) != null) {
                    // For logging purposes
                    storeProtoMessage(msg);

                    String msgProcessed = "Proto msg couldn't be used";

                    if (msg.hasResponse() && swarmAgentInfoViewModel.isAgentInitialized()) {
                        if (msg.getResponse().hasUserCall()) {
                            switch (msg.getResponse().getUserCall().getResponseCase()) {
                                case FUNCTION_LIST_LENGTH:
                                    // TODO
                                    // Create calls to fetch all robot's function
                                    break;
                                case FUNCTION_DESCRIPTION:
                                    // Fetch robot from swarmAgentID
                                    Robot robot = robotListViewModel.getRobotFromList(msg.getSourceId());
                                    if (robot != null) {
                                        // Save to robot its function
                                        MessageOuterClass.FunctionDescription functionDescription = msg.getResponse()
                                                .getUserCall()
                                                .getFunctionDescription()
                                                .getFunctionDescription();
                                        List<MessageOuterClass.FunctionDescriptionArgument> functionArguments =
                                                functionDescription.getArgumentsDescriptionList();

                                        String functionName = functionDescription.getFunctionName();
                                        FunctionTemplate functionTemplate = new FunctionTemplate(functionName);
                                        functionTemplate.setArguments(functionArguments);

                                        robot.addCommand(functionTemplate);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if(msg.hasGreeting()) {
                        int agentID = msg.getGreeting().getId();
                        swarmAgentInfoViewModel.getSwarmAgentID().setValue(agentID);
                    } else if (!swarmAgentInfoViewModel.isAgentInitialized()){ // If receiving data without initialized, send greet again
                        sendGreet();
                    }
                    Log.i(MainActivity.class.getName(), msgProcessed);
                }
            }
        }
    };

    private void storeProtoMessage(@NonNull MessageOuterClass.Message msg)
    {
        protoMsgStorer.addMsg(msg);
        if (!protoMsgStorer.isEmpty()) {
            ProtoMsgViewModel protoMsgViewModel = new ViewModelProvider(this).get(ProtoMsgViewModel.class);
            protoMsgViewModel.getProtoMessages().setValue(protoMsgStorer.toString());
        }
    }

    // TODO update when new details will be available
    private void updateRobots()
    {
        // TODO Retrieve all robots in the swarm
        List<Robot> robotList = new ArrayList<>();

        FunctionTemplate f1 = new FunctionTemplate("Test1");
        f1.addArgument(new FunctionTemplateArgument("Arg int", String.valueOf(0), Integer.class));
        FunctionTemplate f2 = new FunctionTemplate("Test2");
        f2.addArgument(new FunctionTemplateArgument("Arg Float", String.valueOf(0.0f), Float.class));

        Robot robot1 = new Robot("pioneer_0", 0);
        robot1.addCommand(f1);
        robot1.addCommand(f2);

        robotList.add(robot1);
        robotList.add(new Robot("Robot2", 1));
        robotList.add(new Robot("Robot3", 2));

        RobotListViewModel robotListViewModel = new ViewModelProvider(this).get(RobotListViewModel.class);
        robotListViewModel.getRobotList().setValue(robotList);
    }

    public CommunicationDevice getCurrentCommunicationDevice() {return currentCommunicationDevice;}

    public void switchCurrentCommunicationDevice() {
        currentCommunicationDevice.endConnection();
        currentCommunicationDevice.setActive(false);
        if (currentCommunicationDevice instanceof SerialDevice) {
            currentCommunicationDevice = tcpDevice;
        } else if (currentCommunicationDevice instanceof TCPDevice) {
            currentCommunicationDevice = serialDevice;
        }
        currentCommunicationDevice.setActive(true);
    }

    public void sendCommand(@NonNull GenericCommand command) {
        if (swarmAgentInfoViewModel.isAgentInitialized()) {
            currentCommunicationDevice.sendData(command.getCommand(swarmAgentInfoViewModel.getSwarmAgentID().getValue()));
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    public void sendCommand(@NonNull FunctionTemplate function, int swarmAgentDestination) {
        if (swarmAgentInfoViewModel.isAgentInitialized()) {
            currentCommunicationDevice.sendData(function.getProtoMsg(swarmAgentInfoViewModel.getSwarmAgentID().getValue(), swarmAgentDestination));
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendGreet() {
        MessageOuterClass.Greeting greeting = MessageOuterClass.Greeting.newBuilder()
                .setId(0) // TEMP
                .build();

        MessageOuterClass.Message msg = MessageOuterClass.Message.newBuilder()
                .setGreeting(greeting)
                .build();
        currentCommunicationDevice.sendData(msg);
    }
}