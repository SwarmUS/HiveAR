package com.swarmus.hivear.models;

import android.util.Log;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.enums.ConnectionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

// To use the server, you need to provide the ip address and the port on which to create it.
// On the cellphone, the address is simply the ip address of the cellphone and on the emulator,
// use address 10.0.2.15m which corresponds to the emulated network interface in the emulator.
// See https://developer.android.com/studio/run/emulator-networking for details
public class TCPDeviceServer extends CommunicationDevice {
    private int serverPort;
    private String serverBindingAddress;
    private ServerSocket server;
    private Socket clientFd;

    private static final String TCP_INFO_LOG_TAG = "TCP-Server";

    public TCPDeviceServer(String serverBindingAddress, int port) {
        this.serverBindingAddress = serverBindingAddress;
        this.serverPort = port;
    }


    @Override
    public void establishConnection() {
        endConnection();
        currentStatus = ConnectionStatus.connecting;
        broadCastConnectionStatus(ConnectionStatus.connecting);
        Thread connectionThread = new Thread(new ConnectionRunnable());
        connectionThread.start();

    }

    private class ConnectionRunnable implements Runnable {
        public void run() {
            try {

                Log.d(TCP_INFO_LOG_TAG, "Starting server...");

                // Forcing the binding address to be able to set the emulated network interface
                server = new ServerSocket(serverPort, 1, InetAddress.getByName(serverBindingAddress));
                server.setSoTimeout(10000); // Set a connection timeout


                if (server != null) {
                    Log.d(TCP_INFO_LOG_TAG, "Server, awaiting connection");
                    // Accept is blocking and should only return once client connection has been established
                    clientFd = server.accept();
                    Log.d(TCP_INFO_LOG_TAG, "Server obtained client!");
                    currentStatus = ConnectionStatus.connected;
                    connectionCallback.onConnect();
                }
                else {
                    Log.d(TCP_INFO_LOG_TAG, "Failed to start server...");
                    connectionCallback.onConnectError();
                    currentStatus = ConnectionStatus.notConnected;
                    if (server!=null) { server.close(); }
                }

            }
            catch (SocketTimeoutException e) {
                Log.w(TCP_INFO_LOG_TAG, "Connection timeout on server. Closing server...");
                Log.w("TCP", "C: Timeout", e);
                connectionCallback.onConnectError();
            }
            catch (Exception e) {
                Log.e("TCP", "C: Error", e);
                currentStatus = ConnectionStatus.notConnected;
                connectionCallback.onConnectError();
            }
        }
    }

    @Override
    public void endConnection() {
        // End client connect if connected and server if running
        try {
            if (clientFd != null){ clientFd.close();}
            if (server != null){ server.close();}
        } catch (IOException e) {
            Log.e("TCP", "C: Error", e);
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onDisconnect();
    }

    @Override
    public void performConnectionCheck() {
        if (server == null && clientFd == null && currentStatus == ConnectionStatus.connected) {
            endConnection();
        }

    }

    @Override
    public void sendData(byte[] data) {
        // TODO Verify concurrency
        OutputStream outputStream = getSocketOutputStream();
        if (outputStream != null) {
            Thread thread = new Thread(() -> {
                try  {
                    outputStream.write(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        }

    }

    @Override
    public void sendData(String data) {
        sendData(data.getBytes());
    }

    @Override
    public void sendData(MessageOuterClass.Message protoMessage) {
        // TODO: isRunning variable to not write at the same time
        OutputStream outputStream = getSocketOutputStream();
        if (outputStream != null && protoMessage.isInitialized())
        {
            Thread thread = new Thread(() -> {
                try  {
                    protoMessage.writeDelimitedTo(outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        }
    }

    @Override
    public InputStream getDataStream() {
        if (clientFd != null && clientFd.isConnected()) {
            try {
                return clientFd.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onConnectError();
        return null;
    }

    public OutputStream getSocketOutputStream() {
        if (clientFd != null && clientFd.isConnected()) {
            try {
                return clientFd.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onConnectError();
        return null;
    }

    public void setServerPort(int port) {
        this.serverPort=port;}
    public int getServerPort() { return this.serverPort;}

    public void setServerAddress(String address) {
        this.serverBindingAddress=address;}
    public String getServerAddress() { return this.serverBindingAddress;}
}


