package com.bag_tos.client.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executor;
    private MessageListener messageListener;

    public NetworkManager() {
        executor = Executors.newSingleThreadExecutor();
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startListening();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startListening() {
        executor.submit(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (messageListener != null) {
                        messageListener.onMessageReceived(message);
                    }
                }
            } catch (IOException e) {
                if (messageListener != null) {
                    messageListener.onConnectionClosed();
                }
            }
        });
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            executor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionClosed();
    }
}