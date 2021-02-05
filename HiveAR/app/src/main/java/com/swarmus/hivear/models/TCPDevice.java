package com.swarmus.hivear.models;

import android.os.AsyncTask;

import java.io.InputStream;

public class TCPDevice extends CommunicationDevice {
    private TcpClient tcpClient;
    private String serverIP;
    private int serverPort;
    private TcpClient.ClientCallback listener;

    public TCPDevice(String ip, int port) {
        this.serverIP = ip;
        this.serverPort = port;
    }

    @Override
    public void establishConnection() {
        // Close previous connection before creating a new one
        endConnection();
        new ConnectTask().execute("");
    }

    @Override
    public void endConnection() {
        if (tcpClient != null) { tcpClient.stopClient(); }
    }

    @Override
    public void sendData(byte[] data) {
        sendData(data.toString());
    }

    @Override
    public void sendData(String data) {
        if(tcpClient!=null) {tcpClient.sendMessage(data);}
    }

    @Override
    public void removeReadCallBack() {
        this.listener = null;
    }

    @Override
    public InputStream getDataStream() {
        return null;
    }

    public void setClientCallback(TcpClient.ClientCallback clientCallback) {
        this.listener=clientCallback;
    }
    public void setServerIP(String ip) {this.serverIP=ip;}
    public void setServerPort(int port) {this.serverPort=port;}

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            tcpClient = new TcpClient(listener, serverIP, serverPort);
            tcpClient.run();

            return null;
        }
    }
}