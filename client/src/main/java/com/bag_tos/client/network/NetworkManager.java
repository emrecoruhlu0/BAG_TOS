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

/**
 * Sunucu ile JSON formatında iletişim kuran ağ yöneticisi
 */
public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executor;
    private MessageListener messageListener;
    private volatile boolean connected;
    private String serverAddress;
    private int serverPort;

    /**
     * Ağ yöneticisi oluşturur
     */
    public NetworkManager() {
        this.executor = Executors.newSingleThreadExecutor();
        this.connected = false;
    }

    /**
     * Sunucuya bağlanır
     *
     * @param host Sunucu adresi
     * @param port Sunucu portu
     * @return Bağlantı başarılı mı
     */
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

    /**
     * Sunucudan gelen mesajları dinlemeye başlar
     */
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

    /**
     * Gelen mesajı işler
     */
    private void processIncomingMessage(String jsonMessage) {
        if (jsonMessage.trim().isEmpty()) {
            return; // Boş mesajları atla
        }

        try {
            if (jsonMessage.startsWith("{")) {
                // JSON mesajını ayrıştır
                Message message = JsonUtils.parseMessage(jsonMessage);
                if (message != null && messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            } else {
                // Geçersiz format, uyarı ver
                System.err.println("Geçersiz mesaj formatı: " + jsonMessage);
            }
        } catch (Exception e) {
            System.err.println("Mesaj işleme hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Bağlantı hatasını işler
     */
    private void handleConnectionError(Exception e) {
        if (connected) {
            System.err.println("Bağlantı kesildi: " + e.getMessage());
            connected = false;

            if (messageListener != null) {
                messageListener.onConnectionClosed();
            }

            // Otomatik yeniden bağlanma eklenebilir
            // attemptReconnect();
        }
    }

    /**
     * JSON mesajı gönderir
     *
     * @param message Gönderilecek mesaj
     * @return Gönderim başarılı mı
     */
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

    /**
     * Kullanıcı adı gönderir (giriş işlemi)
     *
     * @param username Kullanıcı adı
     */
    public void sendUsername(String username) {
        Message message = new Message(MessageType.READY);
        message.addData("username", username);

        sendMessage(message);
    }

    /**
     * Hazır durumunu gönderir
     *
     * @param isReady Hazır mı
     */
    public void sendReady(boolean isReady) {
        Message message = new Message(MessageType.READY);

        ReadyRequest readyRequest = new ReadyRequest(isReady);
        message.addData("readyRequest", readyRequest);

        sendMessage(message);
    }

    /**
     * Oyun başlatma isteği gönderir
     */
    public void sendStartGame() {
        Message message = new Message(MessageType.START_GAME);

        StartGameRequest startRequest = new StartGameRequest();
        message.addData("startGameRequest", startRequest);

        sendMessage(message);
    }

    /**
     * Oyun aksiyonu gönderir
     *
     * @param actionType Aksiyon tipi
     * @param target Hedef oyuncu
     */
    public void sendAction(ActionType actionType, String target) {
        Message message = new Message(MessageType.ACTION);

        ActionRequest actionRequest = new ActionRequest(actionType.name(), target);
        message.addData("actionRequest", actionRequest);

        sendMessage(message);
    }

    /**
     * Oylama gönderir
     *
     * @param target Hedef oyuncu
     */
    public void sendVote(String target) {
        Message message = new Message(MessageType.VOTE);

        VoteRequest voteRequest = new VoteRequest(target);
        message.addData("voteRequest", voteRequest);

        sendMessage(message);
    }

    /**
     * Sohbet mesajı gönderir
     *
     * @param text Mesaj metni
     * @param room Oda (LOBBY, MAFIA)
     */
    public void sendChatMessage(String text, String room) {
        Message message = new Message(MessageType.CHAT);

        ChatRequest chatRequest = new ChatRequest(text, room);
        message.addData("chatRequest", chatRequest);

        sendMessage(message);
    }

    /**
     * Mesaj dinleyicisini ayarlar
     *
     * @param listener Dinleyici
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Bağlantıyı kapatır
     */
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

    /**
     * Uygulama kapatıldığında kaynakları temizler
     */
    public void shutdown() {
        disconnect();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Bağlantı durumunu döndürür
     *
     * @return Bağlantı durumu
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * Sunucu adresini döndürür
     *
     * @return Sunucu adresi
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Sunucu portunu döndürür
     *
     * @return Sunucu portu
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Mesaj dinleyici arayüzü
     */
    public interface MessageListener {
        /**
         * Mesaj alındığında çağrılır
         *
         * @param message Alınan mesaj
         */
        void onMessageReceived(Message message);

        /**
         * Bağlantı kapandığında çağrılır
         */
        void onConnectionClosed();
    }
}