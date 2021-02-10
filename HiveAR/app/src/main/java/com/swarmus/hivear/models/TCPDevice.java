package com.swarmus.hivear.models;

import android.util.Log;

import com.swarmus.hivear.enums.ConnectionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TCPDevice extends CommunicationDevice {
    private String serverIP;
    private int serverPort;
    private ClientCallback messageListener;
    private Socket socket;

    private static final String TCP_INFO_LOG_TAG = "TCP";

    public TCPDevice(String ip, int port, ClientCallback messageListener) {
        this.serverIP = ip;
        this.serverPort = port;
        this.messageListener = messageListener;
    }

    @Override
    public void establishConnection() {
        // Close previous connection before creating a new one
        endConnection();
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

                if (socket != null && socket.isConnected()) {
                    messageListener.onConnect();
                }
                else {
                    messageListener.onConnectError();
                    if (socket!=null) { socket.close(); }
                }

            } catch (Exception e) {
                Log.e("TCP", "C: Error", e);
                messageListener.onConnectError();
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
        messageListener.onDisconnect();
    }

    @Override
    public void sendData(byte[] data) {
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
        broadCastConnectionStatus(ConnectionStatus.notConnected);
        return null;
    }
    public void setServerIP(String ip) {this.serverIP=ip;}
    public void setServerPort(int port) {this.serverPort=port;}

    public InputStream getSocketInputStream()
    {
        if (socket != null && socket.isConnected())
        {
            try {
                return socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

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
        broadCastConnectionStatus(ConnectionStatus.notConnected);
        return null;
    }

    public interface ClientCallback {
        void onConnect();
        void onDisconnect();
        void onConnectError();
    }
}