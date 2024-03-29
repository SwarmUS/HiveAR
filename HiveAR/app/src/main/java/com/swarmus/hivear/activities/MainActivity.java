package com.swarmus.hivear.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.ar.core.ArCoreApk;
import com.google.protobuf.InvalidProtocolBufferException;
import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.R;
import com.swarmus.hivear.ar.CameraPermissionHelper;
import com.swarmus.hivear.commands.FetchAgentCommands;
import com.swarmus.hivear.commands.GenericCommand;
import com.swarmus.hivear.enums.ConnectionStatus;
import com.swarmus.hivear.enums.FunctionDescriptionState;
import com.swarmus.hivear.models.Agent;
import com.swarmus.hivear.models.CommunicationDevice;
import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.SerialDevice;
import com.swarmus.hivear.models.TCPDeviceServer;
import com.swarmus.hivear.viewmodels.AgentListViewModel;
import com.swarmus.hivear.viewmodels.LocalSwarmAgentViewModel;
import com.swarmus.hivear.viewmodels.ProtoMsgViewModel;
import com.swarmus.hivear.viewmodels.SerialSettingsViewModel;
import com.swarmus.hivear.viewmodels.TcpSettingsViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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

    private LocalSwarmAgentViewModel localSwarmAgentViewModel;
    private static final String BROADCAST_PROTO_MSG_RECEIVED = "Proto Message Received";
    private static final String BROADCAST_PROTO_MSG_TO_SEND = "Proto Message To Send";
    private ProtoMsgViewModel protoMsgViewModel;
    private Queue<MessageOuterClass.Message> receivedMessages;
    private Queue<MessageOuterClass.Message> toSendMessages;

    private AgentListViewModel agentListViewModel;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int DEFAULT_PORT = 700; // Agents in simulation in range 7001+

    private boolean arEnabled = false;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 10*1000; //Delay for 10 seconds.  One second = 1000 milliseconds.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout);


        initViewModels();
        maybeEnableAr(); // Hide AR tab if not possible to do AR
        setUpNavigation();
        setUpCommmunication();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //start handler as activity become visible
        handler.postDelayed( runnable = () -> {
            currentCommunicationDevice.performConnectionCheck();
            handler.postDelayed(runnable, delay);
        }, delay);
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
            Toast.makeText(this, "Camera permission is needed to run AR. Ar features will be turned off.", Toast.LENGTH_LONG)
                    .show();
            hideAr();
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

    private void initViewModels() {
        localSwarmAgentViewModel = new ViewModelProvider(this).get(LocalSwarmAgentViewModel.class);

        agentListViewModel = new ViewModelProvider(this).get(AgentListViewModel.class);
        agentListViewModel.setLocalSwarmAgentViewModel(localSwarmAgentViewModel);
        protoMsgViewModel = new ViewModelProvider(this).get(ProtoMsgViewModel.class);
        registerDefaultProtoMsgStorers();
    }

    private void registerDefaultProtoMsgStorers() {
        // Register all agents logging filter first
        protoMsgViewModel.registerNewProtoMsgStorer(agentListViewModel.getProtoMsgStorer().getValue());
        // Register local logging second
        protoMsgViewModel.registerNewProtoMsgStorer(localSwarmAgentViewModel.getProtoMsgStorer());
    }

    private void hideAr() {
        // Device isn't currently able to use ARCore
        // Hide AR view
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        bottomNavigationView.getMenu().removeItem(R.id.ARViewFragment);
    }

    private void maybeEnableAr() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(this::maybeEnableAr, 200);
        }
        else {
            switch (availability) {
                case UNKNOWN_ERROR:
                case UNKNOWN_CHECKING:
                case UNKNOWN_TIMED_OUT:
                    Toast.makeText(
                            getApplicationContext(),
                            "Cannot detect ARCore on device. AR features will be turned off.",
                            Toast.LENGTH_LONG).show();
                    break;
                case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                    Toast.makeText(
                            getApplicationContext(),
                            "Device doesn't support ARCore. AR features will be turned off.",
                            Toast.LENGTH_LONG).show();
                    break;
                case SUPPORTED_NOT_INSTALLED:
                    Toast.makeText(
                            getApplicationContext(),
                            "ARCore supported but not installed. AR features will be turned off until installed.",
                            Toast.LENGTH_LONG).show();
                    break;
                case SUPPORTED_APK_TOO_OLD:
                    Toast.makeText(
                            getApplicationContext(),
                            "ARCore version too old, please update",
                            Toast.LENGTH_LONG).show();
                    break;
                case SUPPORTED_INSTALLED:
                    arEnabled = true;
                    if (!CameraPermissionHelper.hasCameraPermission(this)) {
                        // Permission denied with checking "Do not ask again".
                        CameraPermissionHelper.requestCameraPermission(this);
                    }
                    break;
            }

            if (!arEnabled) {
                hideAr();
            }
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
                    badgeDrawable.setBackgroundColor(getColor(localSwarmAgentViewModel.isLocalSwarmAgentInitialized() ?
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
        localSwarmAgentViewModel.getLocalSwarmAgentID().observe(this, id -> setConnectionBadge(currentCommunicationDevice.getCurrentStatus()));

        receivedMessages = new LinkedList<>();
        toSendMessages = new LinkedList<>();
        agentListViewModel.getProtoMsgStorer().observe(this, s -> Log.i(TAG, agentListViewModel.getProtoMsgStorer().getValue().getLoggingString(1)));

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
            WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ipA = wifiInfo.getIpAddress();
            ip = Formatter.formatIpAddress(ipA);
        } catch (Exception ex) {
            Log.e("Network", "C: Cannot find host address", ex);
        }

        tcpDevice = new TCPDeviceServer(this, connectionCallback, ip, DEFAULT_PORT);

        TcpSettingsViewModel tcpSettingsViewModel = new ViewModelProvider(this).get(TcpSettingsViewModel.class);
        final Observer<String> ipAddressObserver = s -> ((TCPDeviceServer)tcpDevice).setServerAddress(s);
        tcpSettingsViewModel.getIpAddress().observe(this, ipAddressObserver);
        tcpSettingsViewModel.setIpAddress(ip);

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
        serialSettingsViewModel.setDevices(deviceMap);
        serialSettingsViewModel.setSelectedDevice(
                deviceMap == null || deviceMap.isEmpty() ?
                        "" :
                        deviceMap.values().iterator().next());
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
                        // Only catch, don't log anything as this throws very often
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
            localSwarmAgentViewModel.setLocalSwarmAgentID(
                    LocalSwarmAgentViewModel.DEFAULT_SWARM_AGENT_ID,
                    false);
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
                    if (msg.hasResponse() && localSwarmAgentViewModel.isLocalSwarmAgentInitialized()) {
                        // For logging purposes
                        agentListViewModel.storeNewMsg(msg);
                        if (msg.getResponse().hasUserCall()) {
                            switch (msg.getResponse().getUserCall().getResponseCase()) {
                                case FUNCTION_LIST_LENGTH:
                                    // Create calls to fetch all agent's function
                                    int functionListLength = msg.getResponse().getUserCall().getFunctionListLength().getFunctionArrayLength();
                                    int agentID = msg.getSourceId();
                                    int localID = localSwarmAgentViewModel.getLocalSwarmAgentID().getValue();
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
                                                .setDestinationId(agentID)
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

                                    FunctionDescriptionState functionDescriptionState = getFunctionDescriptionState(msg);

                                    if (!functionDescriptionState.equals(FunctionDescriptionState.VALID)) {
                                        notifyInvalidFunctionDescription(functionDescriptionState, msg);
                                        return;
                                    }

                                    List<MessageOuterClass.FunctionDescriptionArgument> functionArguments =
                                            functionDescription.getArgumentsDescriptionList();
                                    boolean isBuzz =
                                            msg.getResponse().getUserCall().getSourceValue() == MessageOuterClass.UserCallTarget.BUZZ_VALUE;

                                    String functionName = functionDescription.getFunctionName();
                                    FunctionTemplate functionTemplate = new FunctionTemplate(functionName, isBuzz);
                                    functionTemplate.setArguments(functionArguments);

                                    Agent agent = agentListViewModel.getAgentFromList(msg.getSourceId());

                                    if (msg.getSourceId() == localSwarmAgentViewModel.getLocalSwarmAgentID().getValue()) {
                                        localSwarmAgentViewModel.addFunction(functionTemplate);
                                    } else if (agent != null) {
                                        agent.addCommand(functionTemplate);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        } else if (msg.getResponse().hasHivemindHost()) {
                            switch (msg.getResponse().getHivemindHost().getResponseCase()) {
                                case AGENTS_LIST:
                                    // If we get a response, remove all previous entries and use the new ones
                                    agentListViewModel.clearAgentList();
                                    protoMsgViewModel.clearProtoMsgStorers();
                                    registerDefaultProtoMsgStorers();

                                    List<Integer> agentsList =
                                            msg.getResponse().getHivemindHost().getAgentsList().getAgentsList();
                                    for (int agentId : agentsList) {
                                        // Register new agent
                                        Agent agent =
                                                new Agent("Agent #" + Integer.toString(agentId), agentId);
                                        agentListViewModel.addAgent(agent);
                                        protoMsgViewModel.registerNewProtoMsgStorer(agent.getProtoMsgStorer());
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if(msg.hasGreeting()) {
                        int agentID = msg.getGreeting().getAgentId();
                        localSwarmAgentViewModel.setLocalSwarmAgentID(agentID, true);

                        // For logging purposes
                        agentListViewModel.storeNewMsg(msg);

                        // Ask what buzz functions are exposed to device
                        FetchAgentCommands fetchLocalBuzzCommands = new FetchAgentCommands(agentID, true);
                        sendCommand(fetchLocalBuzzCommands);
                    } else if (!localSwarmAgentViewModel.isLocalSwarmAgentInitialized()){ // If receiving data without initialized, send greet again
                        // For logging purposes
                        agentListViewModel.storeNewMsg(msg);
                        sendGreet();
                    }
                }
            }
        }
    };

    private FunctionDescriptionState getFunctionDescriptionState(MessageOuterClass.Message msg) {
        // Check if msg is function description
        if (msg == null ||
            !msg.hasResponse() ||
            !msg.getResponse().hasUserCall() ||
            !msg.getResponse().getUserCall().hasFunctionDescription()) {
            return FunctionDescriptionState.NO_FUNCTION_DESCRIPTION_IN_MSG;
        }

        MessageOuterClass.FunctionDescription functionDescription =
                msg.getResponse().getUserCall().getFunctionDescription().getFunctionDescription();

        // Check if everything is ok to expose to the user
        // Invalid function name
        if (functionDescription.getFunctionName().isEmpty()) {
            return FunctionDescriptionState.MISSING_FUNCTION_NAME;
        }

        // We don't check argument name as the user might understand the function even without those
        // We check if argument if same as the ones supported inside the application
        final boolean[] areArgumentsValid = {true}; // Needs to be array inside of lambda
        functionDescription.getArgumentsDescriptionList().forEach(arg -> {
            if (arg.getType() != MessageOuterClass.FunctionDescriptionArgumentType.INT &&
                    arg.getType() != MessageOuterClass.FunctionDescriptionArgumentType.FLOAT) {
                areArgumentsValid[0] = false;
            }
        });

        // All checks passed if arguments are valid
        return areArgumentsValid[0] ? FunctionDescriptionState.VALID : FunctionDescriptionState.INVALID_ARGUMENT;
    }

    private void notifyInvalidFunctionDescription(FunctionDescriptionState state, MessageOuterClass.Message msg) {
        switch (state) {
            case VALID:
                // Don't notify on valid function description
                break;
            case MISSING_FUNCTION_NAME:
                Toast.makeText(
                        getApplicationContext(),
                        "Received a function description with no name. " +
                                "Please look into the logs for more information.",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error in name of function description for message: " + msg);
                break;
            case INVALID_ARGUMENT:
                Toast.makeText(
                        getApplicationContext(),
                        "Received a function description with invalid argument type. " +
                                "Please look into the logs for more information.",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error in argument type for message: " + msg);
                break;
            case NO_FUNCTION_DESCRIPTION_IN_MSG:
                Toast.makeText(
                        getApplicationContext(),
                        "Received a function description with error. " +
                                "Please look into the logs for more information.",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error function description for message: " + msg);
                break;
        }
    }

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
        if (localSwarmAgentViewModel.isLocalSwarmAgentInitialized()) {
            toSendMessages.add(command.getCommand(localSwarmAgentViewModel.getLocalSwarmAgentID().getValue()));
            Intent msgToSendIntent = new Intent();
            msgToSendIntent.setAction(BROADCAST_PROTO_MSG_TO_SEND);
            sendBroadcast(msgToSendIntent);
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    public void sendCommand(@NonNull FunctionTemplate function, int swarmAgentDestination) {
        if (localSwarmAgentViewModel.isLocalSwarmAgentInitialized()) {
            toSendMessages.add(function.getProtoMsg(localSwarmAgentViewModel.getLocalSwarmAgentID().getValue(), swarmAgentDestination));
            Intent msgToSendIntent = new Intent();
            msgToSendIntent.setAction(BROADCAST_PROTO_MSG_TO_SEND);
            sendBroadcast(msgToSendIntent);
        }
        else {
            Toast.makeText(this, "Swarm Agent not initialized, can't send command.", Toast.LENGTH_LONG).show();
        }
    }

    public void sendCommand(@NonNull MessageOuterClass.Message msg) {
        if (localSwarmAgentViewModel.isLocalSwarmAgentInitialized()) {
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
            agentListViewModel.storeSentCommand(msg);
        } else {
            Toast.makeText(this, "Incorrect Command to send.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendGreet() {
        MessageOuterClass.Greeting greeting = MessageOuterClass.Greeting.newBuilder()
                .setAgentId(0) // TEMP
                .build();

        MessageOuterClass.Message msg = MessageOuterClass.Message.newBuilder()
                .setGreeting(greeting)
                .build();
        currentCommunicationDevice.sendData(msg);
    }
}