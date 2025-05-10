package com.bag_tos.client.network;

import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.*;
import com.bag_tos.common.model.ActionType;
import com.bag_tos.common.util.JsonUtils;

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
    private volatile boolean connected;
    private String serverAddress;
    private int serverPort;

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
            if (message != null && messageListener != null) {
                messageListener.onMessageReceived(message);
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

    // Kullanıcı adı gönderimi için yardımcı metot
    public void sendAuthMessage(String username) {
        Message message = new Message(MessageType.READY);
        message.addData("username", username);
        sendMessage(message);
    }

    // Hazır durumu için yardımcı metot
    public boolean sendReadyMessage(boolean isReady) {
        Message message = new Message(MessageType.READY);
        ReadyRequest readyRequest = new ReadyRequest(isReady);
        message.addData("readyRequest", readyRequest);
        return sendMessage(message);
    }

    // Oyun başlatma için yardımcı metot
    public boolean sendStartGameMessage() {
        Message message = new Message(MessageType.START_GAME);
        StartGameRequest startRequest = new StartGameRequest();
        message.addData("startGameRequest", startRequest);
        return sendMessage(message);
    }

    // Aksiyon için yardımcı metot
    public boolean sendActionMessage(ActionType actionType, String target) {
        Message message = new Message(MessageType.ACTION);
        ActionRequest actionRequest = new ActionRequest(actionType.name(), target);
        message.addData("actionRequest", actionRequest);
        return sendMessage(message);
    }

    // Oylama için yardımcı metot
    public boolean sendVoteMessage(String target) {
        Message message = new Message(MessageType.VOTE);
        VoteRequest voteRequest = new VoteRequest(target);
        message.addData("voteRequest", voteRequest);
        return sendMessage(message);
    }

    // Sohbet için yardımcı metot
    public boolean sendChatMessage(String text, String room) {
        Message message = new Message(MessageType.CHAT);
        ChatRequest chatRequest = new ChatRequest(text, room);
        message.addData("chatRequest", chatRequest);
        return sendMessage(message);
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

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public interface MessageListener {
        void onMessageReceived(Message message);
        void onConnectionClosed();
    }
}