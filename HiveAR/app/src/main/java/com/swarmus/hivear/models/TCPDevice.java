package com.swarmus.hivear.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPDevice implements CommunicationDevice {
    private Thread connectionThread;

    private Socket socket;
    private OutputStream socketOutput;
    private BufferedReader socketInput;

    private String ip;
    private int port;
    private ClientCallback listener=null;

    public TCPDevice(String ip, int port){
        this.ip=ip;
        this.port=port;
    }

    @Override
    public void init() {
    }

    @Override
    public void establishConnection() {
        // Reset socket for new connection
        if (socket.isConnected())
        {
            // Ends previous thread by closing socket
            endConnection();
        }
        new Thread(() -> {
            socket = new Socket();
            InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
            try {
                socket.connect(socketAddress);
                socketOutput = socket.getOutputStream();
                socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                new ReceiveThread().start();

                if(listener!=null)
                    listener.onConnect(socket);
            } catch (IOException e) {
                if(listener!=null)
                    listener.onConnectError(socket, e.getMessage());
            }
        }).start();
    }

    @Override
    public void endConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            if(listener!=null)
                listener.onDisconnect(socket, e.getMessage());
        }
    }

    @Override
    public void sendData(byte[] data) {
        try {
            socketOutput.write(data);
        } catch (IOException e) {
            if(listener!=null)
                listener.onDisconnect(socket, e.getMessage());
        }
    }

    @Override
    public void removeReadCallBack() {
        this.listener=null;
    }

    public void setIp(String ip) {
        this.ip=ip;
        endConnection();
        establishConnection();
    }

    public void setPort(int port) {
        this.port=port;
        endConnection();
        establishConnection();
    }

    private class ReceiveThread extends Thread implements Runnable{
        public void run(){
            String message;
            try {
                // TODO binary data won't contain \n in the future, invalidating readline() call here
                while((message = socketInput.readLine()) != null) {   // each line must end with a \n to be received
                    if(listener!=null)
                        listener.onMessage(message);
                }
            } catch (IOException e) {
                if(listener!=null)
                    listener.onDisconnect(socket, e.getMessage());
            }
        }
    }

    public void setClientCallback(ClientCallback listener){
        this.listener=listener;
    }

    public interface ClientCallback {
        void onMessage(String message);
        void onConnect(Socket socket);
        void onDisconnect(Socket socket, String message);
        void onConnectError(Socket socket, String message);
    }
}