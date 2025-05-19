package com.bag_tos.client.network;

import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.*;
import com.bag_tos.common.model.ActionType;
import com.bag_tos.common.util.JsonUtils;
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executor;
    private MessageListener messageListener;
    private volatile boolean connected;
    private String serverAddress;
    private int serverPort;

    private BlockingQueue<Message> responseQueue = new LinkedBlockingQueue<>();


    public NetworkManager() {
        this.executor = Executors.newSingleThreadExecutor();
        this.connected = false;
    }

    public boolean connect(String host, int port) {
        try {
            this.serverAddress = host;
            this.serverPort = port;

            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;
            startListening();

            System.out.println("Sunucuya bağlandı: " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Bağlantı hatası: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    private void startListening() {
        executor.submit(() -> {
            try {
                String jsonMessage;
                while (connected && (jsonMessage = in.readLine()) != null) {
                    processIncomingMessage(jsonMessage);
                }
            } catch (IOException e) {
                handleConnectionError(e);
            }
        });
    }

    private void processIncomingMessage(String jsonMessage) {
        if (jsonMessage.trim().isEmpty()) {
            return; // Boş mesajları atla
        }

        try {
            Message message = JsonUtils.parseMessage(jsonMessage);
            if (message != null) {
                // Mesajı kuyruğa ekle
                responseQueue.offer(message);

                // Dinleyiciler varsa onları bilgilendir
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Mesaj işleme hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConnectionError(Exception e) {
        if (connected) {
            System.err.println("Bağlantı kesildi: " + e.getMessage());
            connected = false;

            if (messageListener != null) {
                messageListener.onConnectionClosed();
            }
        }
    }

    public boolean sendMessage(Message message) {
        if (out != null && connected) {
            try {
                String jsonString = JsonUtils.toJson(message);
                if (jsonString != null) {
                    out.println(jsonString);
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Mesaj gönderme hatası: " + e.getMessage());
            }
        }
        return false;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void disconnect() {
        if (!connected) {
            return; // Zaten bağlı değil
        }

        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Bağlantı kapatma hatası: " + e.getMessage());
        }

        System.out.println("Sunucu bağlantısı kapatıldı.");
    }

    public void shutdown() {
        disconnect();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public interface MessageListener {
        void onMessageReceived(Message message);
        void onConnectionClosed();
    }
}