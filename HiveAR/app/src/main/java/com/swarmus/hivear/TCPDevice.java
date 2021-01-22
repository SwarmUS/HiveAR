package com.swarmus.hivear;

import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPDevice implements CommunicationDevice {
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
        removeClientCallback();
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
    public byte[] getData() {
        return new byte[0];
    }

    private class ReceiveThread extends Thread implements Runnable{
        public void run(){
            String message;
            try {
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

    public void removeClientCallback(){
        this.listener=null;
    }

    public interface ClientCallback {
        void onMessage(String message);
        void onConnect(Socket socket);
        void onDisconnect(Socket socket, String message);
        void onConnectError(Socket socket, String message);
    }
}

// Example of TCP client inside of Activity
/*
        TCPDevice client = new TCPDevice("192.168.0.26", 3000);
        client.setClientCallback(new TCPDevice.ClientCallback () {
            @Override
            public void onMessage(String message) {
                Log.d("HEY", "new message: "+message);
            }

            @Override
            public void onConnect(Socket socket) {
                Log.d("HEY", "new connection");
            }

            @Override
            public void onDisconnect(Socket socket, String message) {
                Log.d("HEY", "new disconnect: "+message);
                client.connect();
            }

            @Override
            public void onConnectError(Socket socket, String message) {
                Log.d("HEY", "Can't connect: "+message);
            }
        });

        client.connect();
 */