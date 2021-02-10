package com.swarmus.hivear.models;

// From https://stackoverflow.com/a/38163121

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {

    public static final String TAG = TcpClient.class.getSimpleName();
    private String serverIP;
    private int serverPort;
    private String serverMessage;
    private ClientCallback messageListener;
    private boolean isRunning = false;
    private PrintWriter bufferOutput;
    private BufferedReader bufferInput;


    public TcpClient(ClientCallback listener, String serverIP, int serverPort) {
        messageListener = listener;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void sendMessage(final String message) {
        Runnable runnable = () -> {
            if (bufferOutput != null) {
                Log.d(TAG, "Sending: " + message);
                bufferOutput.print(message);
                bufferOutput.flush();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void stopClient() {

        isRunning = false;

        if (bufferOutput != null) {
            bufferOutput.flush();
            bufferOutput.close();
        }

        bufferInput = null;
        bufferOutput = null;
        serverMessage = null;
        messageListener.onDisconnect();
    }

    public void run() {

        isRunning = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(serverIP);

            Log.d("TCP Client", "C: Connecting...");

            Socket socket = new Socket(serverAddr, serverPort);

            try {

                //sends the message to the server
                bufferOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                bufferInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                if ((bufferInput != null) && (bufferOutput != null)) {
                    messageListener.onConnect();}

                //in this while the client listens for the messages sent by the server
                while (isRunning) {

                    serverMessage = bufferInput.readLine();

                    if (serverMessage != null && messageListener != null) {
                        messageListener.onMessage(serverMessage);
                    }

                }

                Log.d("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");

            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
                messageListener.onConnectError();
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
                messageListener.onDisconnect();
            }

        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
            messageListener.onConnectError();
        }

    }

    public interface ClientCallback {
        void onMessage(String message);
        void onConnect();
        void onDisconnect();
        void onConnectError();
    }

}