package com.bag_tos;

import com.bag_tos.common.audio.AudioFormat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Town of Salem benzeri oyun için sunucu uygulaması
 */
public class Server {
    private static final int PORT = 1234;
    private static final int THREAD_POOL_SIZE = 10;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private RoomHandler roomHandler;
    private ExecutorService threadPool;
    private boolean running;

    // YENİ: Ses sunucusu eklendi
    private VoiceServer voiceServer;

    /**
     * Sunucuyu başlatır
     */
    public void start() {
        try {
            // Sunucu soketi oluştur
            serverSocket = new ServerSocket(PORT);
            clients = new ArrayList<>();
            roomHandler = new RoomHandler();
            threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            running = true;

            System.out.println("Sunucu başladı. Port: " + PORT);

            // YENİ: Ses sunucusunu başlat
            voiceServer = new VoiceServer(AudioFormat.DEFAULT_VOICE_PORT, roomHandler);
            voiceServer.start();
            System.out.println("Ses sunucusu başladı. Port: " + AudioFormat.DEFAULT_VOICE_PORT);

            // Bağlantıları kabul et
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewConnection(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Bağlantı kabul edilirken hata: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Sunucu başlatılırken hata: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Yeni bağlantıyı işler
     */
    private void handleNewConnection(Socket clientSocket) {
        // Bu metot aynı kalacak
    }

    /**
     * Sunucuyu kapatır
     */
    public void shutdown() {
        running = false;

        // İstemci bağlantılarını kapat
        for (ClientHandler client : clients) {
            try {
                client.cleanup();
            } catch (Exception e) {
                // Yok sayılabilir
            }
        }

        // Thread havuzunu kapat
        if (threadPool != null) {
            threadPool.shutdown();
        }

        // Sunucu soketini kapat
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Yok sayılabilir
            }
        }

        // YENİ: Ses sunucusunu kapat
        if (voiceServer != null) {
            voiceServer.stop();
            voiceServer = null;
        }

        System.out.println("Sunucu kapatıldı.");
    }

    /**
     * Ana metot
     */
    public static void main(String[] args) {
        Server server = new Server();

        // Güvenli kapatma için shutdown hook ekle
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        // Sunucuyu başlat
        server.start();
    }
}