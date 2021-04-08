package com.swarmus.hivear.models;

import android.content.Context;
import android.util.Log;

import com.swarmus.hivear.MessageOuterClass;
import com.swarmus.hivear.enums.ConnectionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPDeviceClient extends CommunicationDevice {
    private String serverIP;
    private int serverPort;
    private Socket socket;

    private static final String TCP_INFO_LOG_TAG = "TCP-Client";

    public TCPDeviceClient(Context context, ConnectionCallback connectionCallback, String ip, int port) {
        super(context, connectionCallback);
        this.serverIP = ip;
        this.serverPort = port;
    }

    @Override
    public void establishConnection() {
        // Close previous connection before creating a new one
        endConnection();
        currentStatus = ConnectionStatus.connecting;
        broadCastConnectionStatus(ConnectionStatus.connecting);
        Thread connectionThread = new Thread(new ConnectionRunnable());
        connectionThread.start();
    }

    private class ConnectionRunnable implements Runnable {
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverIP);

                Log.d(TCP_INFO_LOG_TAG, "Connecting...");

                socket = new Socket(serverAddr, serverPort);
                socket.setSoTimeout(10000); // Max time to connect = 10 seconds

                if (socket != null && socket.isConnected()) {
                    currentStatus = ConnectionStatus.connected;
                    connectionCallback.onConnect();
                }
                else {
                    connectionCallback.onConnectError();
                    currentStatus = ConnectionStatus.notConnected;
                    if (socket!=null) { socket.close(); }
                }

            } catch (SocketTimeoutException e) {
                Log.w(TCP_INFO_LOG_TAG, "Connection timeout.");
                currentStatus = ConnectionStatus.notConnected;
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
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onDisconnect();
    }

    @Override
    public void performConnectionCheck() {
        if (currentStatus == ConnectionStatus.connected) {
            if (socket == null ||
                socket.isClosed() ||
                !socket.isConnected() ||
                socket.isInputShutdown() ||
                socket.isOutputShutdown()) {
                endConnection();
            }
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
    public InputStream getDataStream()
    {
        if (socket != null && socket.isConnected())
        {
            try {
                return socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onConnectError();
        return null;
    }
    public void setServerIP(String ip) {this.serverIP=ip;}
    public void setServerPort(int port) {this.serverPort=port;}

    public String getServerIP() {return serverIP;}
    public int getServerPort() {return serverPort;}

    public OutputStream getSocketOutputStream()
    {
        if (socket != null && socket.isConnected())
        {
            try {
                return socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentStatus = ConnectionStatus.notConnected;
        connectionCallback.onConnectError();
        return null;
    }
}