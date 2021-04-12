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
import com.google.protobuf.InvalidProtocolBufferException;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.R;
import com.swarmus.hivear.ar.CameraPermissionHelper;
import com.swarmus.hivear.commands.FetchRobotCommands;
import com.swarmus.hivear.commands.GenericCommand;
import com.swarmus.hivear.enums.ConnectionStatus;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.TCPDeviceServer;
import com.swarmus.hivear.viewmodels.ProtoMsgViewModel;
import com.swarmus.hivear.viewmodels.RobotListViewModel;
import com.swarmus.hivear.viewmodels.SerialSettingsViewModel;
import com.swarmus.hivear.viewmodels.SettingsViewModel;
import com.swarmus.hivear.viewmodels.SwarmAgentInfoViewModel;
import com.swarmus.hivear.viewmodels.TcpSettingsViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
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
    private static final String BROADCAST_PROTO_MSG_TO_SEND = "Proto Message To Send";
    private ProtoMsgViewModel protoMsgViewModel;
    private Queue<MessageOuterClass.Message> receivedMessages;
    private Queue<MessageOuterClass.Message> toSendMessages;

    private RobotListViewModel robotListViewModel;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int DEFAULT_PORT = 700; // Agents in simulation in range 7001+

    private boolean userRequestedInstall = true;
    private boolean arEnabled = false;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 10*1000; //Delay for 10 seconds.  One second = 1000 milliseconds.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout);

        maybeEnableAr(); // Hide AR tab if not possible to do AR
        setUpNavigation();
        setUpCommmunication();
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

        //start handler as activity become visible
        handler.postDelayed( runnable = () -> {
            currentCommunicationDevice.performConnectionCheck();
            handler.postDelayed(runnable, delay);
        }, delay);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    // If onPause() is not included the threads will double up when you reload the activity

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
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
                    badgeDrawable.setBackgroundColor(getColor(swarmAgentInfoViewModel.isAgentInitialized() ?
                            R.color.connection_established :
                            R.color.connection_established_no_swarm));
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
        swarmAgentInfoViewModel.getSwarmAgentID().observe(this, id -> setConnectionBadge(currentCommunicationDevice.getCurrentStatus()));

        protoMsgViewModel = new ViewModelProvider(this).get(ProtoMsgViewModel.class);

        receivedMessages = new LinkedList<>();
        toSendMessages = new LinkedList<>();
        ProtoMsgViewModel protoMsgViewModel = new ViewModelProvider(this).get(ProtoMsgViewModel.class);
        protoMsgViewModel.getMsgQueue().observe(this, s -> Log.i(TAG, protoMsgViewModel.getLastMsgs(1)));

        IntentFilter filterProtoMsgReceived = new IntentFilter(BROADCAST_PROTO_MSG_RECEIVED);
        registerReceiver(protoMsgReadReceiver, filterProtoMsgReceived);

        IntentFilter filterProtoMsgToSend = new IntentFilter(BROADCAST_PROTO_MSG_TO_SEND);
        registerReceiver(protoMsgWriteReceiver, filterProtoMsgToSend);

        IntentFilter filterConnectionStatus = new IntentFilter(CommunicationDevice.CONNECTION_STATUS_RESULT);
        registerReceiver(deviceConnectionStatusReceiver, filterConnectionStatus);

        setUpTCPCommunication();
        setUpSerialCommunication();

        currentCommunicationDevice = serialDevice;
        currentCommunicationDevice.setActive(true);
    }

    private void setUpTCPCommunication() {
        String ip = "0.0.0.0";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Network", "C: Cannot find host address", ex);
        }

        tcpDevice = new TCPDeviceServer(this, connectionCallback, ip, DEFAULT_PORT);

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> ((TCPDeviceServer)tcpDevice).setServerAddress(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);

        final Observer<Integer> portObserver = p -> ((TCPDeviceServer)tcpDevice).setServerPort(p);
        tcpSettingsViewModel.getPort().observe(this, portObserver);
    }

    private void setUpSerialCommunication() {
        IntentFilter filter = new IntentFilter(SerialDevice.ACTION_SERIAL_DEVICE_CHANGED);
        registerReceiver(serialDeviceChangedReceiver, filter);

        SerialSettingsViewModel serialSettingsViewModel = new ViewModelProvider(this).get(SerialSettingsViewModel.class);
        serialDevice = new SerialDevice(this, connectionCallback);
        serialSettingsViewModel.getSelectedDevice().observe(this, deviceName -> ((SerialDevice)serialDevice).setSelectedUsbDeviceName(deviceName));
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
                        if (msg != null) {
                            receivedMessages.add(msg);
                            sendBroadcast(msgReceivedIntent);
                        }
                    } catch(InvalidProtocolBufferException e) {
                        Log.w(TAG, "Unfinished message " + e.getUnfinishedMessage());
                    }
                    catch (IOException e) {
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
            swarmAgentInfoViewModel.setSwarmAgentID(SwarmAgentInfoViewModel.DEFAULT_SWARM_AGENT_ID);
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

                    protoMsgViewModel.addMsg(msg);

                    if (msg.hasResponse() && swarmAgentInfoViewModel.isAgentInitialized()) {
                        if (msg.getResponse().hasUserCall()) {
                            switch (msg.getResponse().getUserCall().getResponseCase()) {
                                case FUNCTION_LIST_LENGTH:
                                    // Create calls to fetch all robot's function
                                    int functionListLength = msg.getResponse().getUserCall().getFunctionListLength().getFunctionArrayLength();
                                    int robotID = msg.getSourceId();
                                    int localID = swarmAgentInfoViewModel.getSwarmAgentID().getValue();
                                    MessageOuterClass.UserCallTarget destination = msg.getResponse().getUserCall().getSource();

                                    for (int i = 0; i < functionListLength; i++) {
                                        MessageOuterClass.FunctionDescriptionRequest functionDescriptionRequest = MessageOuterClass.FunctionDescriptionRequest.newBuilder()
                                                .setFunctionListIndex(i)
                                                .build();

                                        MessageOuterClass.UserCallRequest userCallRequest = MessageOuterClass.UserCallRequest.newBuilder()
                                                .setDestination(destination)
                                                .setSource(MessageOuterClass.UserCallTarget.HOST)
                                                .setFunctionDescription(functionDescriptionRequest)
                                                .build();
                                        MessageOuterClass.Request request = MessageOuterClass.Request.newBuilder()
                                                .setUserCall(userCallRequest).build();
                                        MessageOuterClass.Message message = MessageOuterClass.Message.newBuilder()
                                                .setDestinationId(robotID)
                                                .setSourceId(localID)
                                                .setRequest(request)
                                                .build();
                                        sendCommand(message);
                                    }

                                    break;
                                case FUNCTION_DESCRIPTION:
                                    MessageOuterClass.FunctionDescription functionDescription = msg.getResponse()
                                            .getUserCall()
                                            .getFunctionDescription()
                                            .getFunctionDescription();
                                    List<MessageOuterClass.FunctionDescriptionArgument> functionArguments =
                                            functionDescription.getArgumentsDescriptionList();
                                    boolean isBuzz =
                                            msg.getResponse().getUserCall().getSourceValue() == MessageOuterClass.UserCallTarget.BUZZ_VALUE;

                                    String functionName = functionDescription.getFunctionName();
                                    FunctionTemplate functionTemplate = new FunctionTemplate(functionName, isBuzz);
                                    functionTemplate.setArguments(functionArguments);

                                    Robot robot = robotListViewModel.getRobotFromList(msg.getSourceId());

                                    if (msg.getSourceId() == swarmAgentInfoViewModel.getSwarmAgentID().getValue()) {
                                        swarmAgentInfoViewModel.addFunction(functionTemplate);
                                    } else if (robot != null) {
                                        robot.addCommand(functionTemplate);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if(msg.hasGreeting()) {
                        int agentID = msg.getGreeting().getId();
                        swarmAgentInfoViewModel.setSwarmAgentID(agentID);
                        // Ask what buzz functions are exposed to device
                        FetchRobotCommands fetchLocalBuzzCommands = new FetchRobotCommands(agentID, true);
                        sendCommand(fetchLocalBuzzCommands);
                    } else if (!swarmAgentInfoViewModel.isAgentInitialized()){ // If receiving data without initialized, send greet again
                        sendGreet();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver protoMsgWriteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BROADCAST_PROTO_MSG_TO_SEND.equals(action)) {
                MessageOuterClass.Message msg;
                while ((msg = toSendMessages.poll()) != null) {
                    sendProtoMsg(msg);
                    Log.i(TAG, "Sending message: " + msg);
                }
            }
        }
    };

    // TODO update when new details will be available
    private void updateRobots()
    {
        robotListViewModel = new ViewModelProvider(this).get(RobotListViewModel.class);

        // TODO Retrieve all robots in the swarm
        List<Robot> robotList = new ArrayList<>();
        robotList.add(new Robot("pioneer_0", 1));
        robotList.add(new Robot("pioneer_1", 2));
        robotList.add(new Robot("pioneer_2", 3));

        RobotListViewModel robotListViewModel = new ViewModelProvider(this).get(RobotListViewModel.class);
        robotListViewModel.getRobotList().setValue(robotList);
    }

    public CommunicationDevice getCurrentCommunicationDevice() {return currentCommunicationDevice;}

    public void switchCurrentCommunicationDevice() {
        currentCommunicationDevice.endConnection();
        currentCommunicationDevice.setActive(false);
        if (currentCommunicationDevice instanceof SerialDevice) {
            currentCommunicationDevice = tcpDevice;
        } else if (currentCommunicationDevice instanceof TCPDeviceServer) {
            currentCommunicationDevice = serialDevice;
        }
        currentCommunicationDevice.setActive(true);
    }

    public void sendCommand(@NonNull GenericCommand command) {
        if (swarmAgentInfoViewModel.isAgentInitialized()) {
            toSendMessages.add(command.getCommand(swarmAgentInfoViewModel.getSwarmAgentID().getValue()));
            Intent msgToSendIntent = new Intent();
            msgToSendIntent.setAction(BROADCAST_PROTO_MSG_TO_SEND);
            sendBroadcast(msgToSendIntent);
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    public void sendCommand(@NonNull FunctionTemplate function, int swarmAgentDestination) {
        if (swarmAgentInfoViewModel.isAgentInitialized()) {
            toSendMessages.add(function.getProtoMsg(swarmAgentInfoViewModel.getSwarmAgentID().getValue(), swarmAgentDestination));
            Intent msgToSendIntent = new Intent();
            msgToSendIntent.setAction(BROADCAST_PROTO_MSG_TO_SEND);
            sendBroadcast(msgToSendIntent);
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    public void sendCommand(@NonNull MessageOuterClass.Message msg) {
        if (swarmAgentInfoViewModel.isAgentInitialized()) {
            toSendMessages.add(msg);
            Intent msgToSendIntent = new Intent();
            msgToSendIntent.setAction(BROADCAST_PROTO_MSG_TO_SEND);
            sendBroadcast(msgToSendIntent);
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendProtoMsg(MessageOuterClass.Message msg) {
        if (msg != null) {
            currentCommunicationDevice.sendData(msg);
        } else {
            Toast.makeText(this, "Incorrect Command to send.", Toast.LENGTH_LONG).show();
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