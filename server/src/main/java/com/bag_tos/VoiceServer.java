package com.bag_tos;

import com.bag_tos.common.audio.AudioFormat;
import com.bag_tos.common.audio.VoiceCommand;
import com.bag_tos.common.audio.VoicePacket;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Sesli iletişim için UDP tabanlı sunucu.
 * Bu sınıf, mevcut oyun sunucusu ile birlikte çalışır ve
 * sesli iletişim için ayrı bir port üzerinden hizmet verir.
 */
public class VoiceServer implements Runnable {
    private final int port;
    private final RoomHandler roomHandler;
    private final VoiceRoomHandler voiceRoomHandler;
    private boolean running = false;
    private DatagramSocket socket;

    // Bağlı kullanıcıları izleme
    private final Map<String, VoiceClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Thread havuzu
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Ses sunucusu oluşturur ve başlatır
     * @param port Bağlantı portu
     * @param roomHandler Oyun oda yöneticisi
     */
    public VoiceServer(int port, RoomHandler roomHandler) {
        this.port = port;
        this.roomHandler = roomHandler;
        this.voiceRoomHandler = new VoiceRoomHandler(roomHandler);
    }

    /**
     * Sunucuyu başlatır
     */
    public void start() {
        try {
            socket = new DatagramSocket(port);
            running = true;
            Thread serverThread = new Thread(this);
            serverThread.setDaemon(true);
            serverThread.start();
            System.out.println("Voice Server başlatıldı, port: " + port);
        } catch (SocketException e) {
            System.err.println("Voice Server başlatılamadı: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sunucuyu durdurur
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        System.out.println("Voice Server durduruldu");
    }

    @Override
    public void run() {
        byte[] buffer = new byte[AudioFormat.BUFFER_SIZE];

        while (running) {
            try {
                // Paket al
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Paketi işle (ayrı bir thread'de)
                threadPool.submit(() -> processPacket(packet));

            } catch (IOException e) {
                if (running) {
                    System.err.println("Paket alınırken hata: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gelen UDP paketini işler
     * @param packet Gelen UDP paketi
     */
    private void processPacket(DatagramPacket packet) {
        try {
            // Veriyi deserialize et
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);

            Object receivedObject = ois.readObject();

            if (receivedObject instanceof VoicePacket) {
                handleVoicePacket((VoicePacket) receivedObject, packet.getAddress(), packet.getPort());
            } else if (receivedObject instanceof VoiceCommand) {
                handleVoiceCommand((VoiceCommand) receivedObject, packet.getAddress(), packet.getPort());
            } else {
                System.err.println("Bilinmeyen nesne tipi: " + receivedObject.getClass().getName());
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Paket işlenirken hata: " + e.getMessage());
        }
    }

    /**
     * Ses paketini işler ve uygun alıcılara yönlendirir
     * @param voicePacket Ses paketi
     * @param address Gönderenin IP adresi
     * @param port Gönderenin portu
     */
    private void handleVoicePacket(VoicePacket voicePacket, InetAddress address, int port) {
        String username = voicePacket.getUsername();
        String roomName = voicePacket.getRoomName();

        // Kullanıcı bilgilerini güncelle
        updateClientInfo(username, address, port);

        // Oyun durumuna göre paketi yönlendir
        if (voiceRoomHandler.canTalk(username, roomName)) {
            broadcastVoicePacket(voicePacket, username);
        }
    }

    /**
     * Komut paketini işler
     * @param command Komut paketi
     * @param address Gönderenin IP adresi
     * @param port Gönderenin portu
     */
    private void handleVoiceCommand(VoiceCommand command, InetAddress address, int port) {
        String username = command.getUsername();

        // Kullanıcı bilgilerini güncelle
        updateClientInfo(username, address, port);

        // Komut tipine göre işle
        switch (command.getType()) {
            case JOIN:
                voiceRoomHandler.playerJoinedRoom(username, command.getRoomName());
                break;

            case LEAVE:
                voiceRoomHandler.playerLeftRoom(username, command.getRoomName());
                break;

            case MUTE:
                voiceRoomHandler.playerMuted(username);
                break;

            case UNMUTE:
                voiceRoomHandler.playerUnmuted(username);
                break;

            case HEARTBEAT:
                // Bağlantı aktif, bir şey yapmaya gerek yok
                break;

            case PHASE_CHANGE:
                boolean isNight = "NIGHT".equals(command.getExtraData());
                voiceRoomHandler.setNightPhase(isNight);
                break;

            case PLAYER_DIED:
                voiceRoomHandler.playerDied(username);
                break;

            case PING:
                // Gecikme ölçümü isteği geldi, PONG ile yanıt ver
                sendPongResponse(username, address, port, command.getTimestamp());
                break;
        }
    }

    /**
     * Gecikme ölçümü yanıtı gönderir
     */
    private void sendPongResponse(String username, InetAddress address, int port, long pingTimestamp) {
        try {
            VoiceCommand pongCommand = new VoiceCommand(VoiceCommand.CommandType.PONG, username);
            pongCommand.setTimestamp(pingTimestamp); // Aynı timestamp'i geri gönder

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(pongCommand);

            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);

        } catch (IOException e) {
            System.err.println("PONG yanıtı gönderilirken hata: " + e.getMessage());
        }
    }

    /**
     * İstemci bilgilerini günceller veya yeni istemci ekler
     * @param username Kullanıcı adı
     * @param address IP adresi
     * @param port Port numarası
     */
    private void updateClientInfo(String username, InetAddress address, int port) {
        if (!connectedClients.containsKey(username)) {
            // Yeni istemci
            VoiceClientHandler client = new VoiceClientHandler(username, address, port);
            connectedClients.put(username, client);
            System.out.println("Voice Client bağlandı: " + username + " (" + address.getHostAddress() + ":" + port + ")");
        } else {
            // Mevcut istemciyi güncelle
            VoiceClientHandler client = connectedClients.get(username);
            client.updateConnectionInfo(address, port);
        }
    }

    /**
     * Ses paketini diğer istemcilere yayınlar
     * @param voicePacket Ses paketi
     * @param senderUsername Gönderenin kullanıcı adı
     */
    private void broadcastVoicePacket(VoicePacket voicePacket, String senderUsername) {
        try {
            // Paketi serialize et
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(voicePacket);

            byte[] data = baos.toByteArray();

            // Aynı odadaki diğer oyunculara gönder
            String roomName = voicePacket.getRoomName();
            List<String> receivers = voiceRoomHandler.getPlayersInRoom(roomName);

            for (String receiverUsername : receivers) {
                // Kendine gönderme
                if (receiverUsername.equals(senderUsername)) {
                    continue;
                }

                // Eğer oyuncu konuşabiliyor ve dinleyebiliyorsa gönder
                if (voiceRoomHandler.canListen(receiverUsername, roomName)) {
                    VoiceClientHandler receiver = connectedClients.get(receiverUsername);
                    if (receiver != null) {
                        DatagramPacket packet = new DatagramPacket(
                                data, data.length,
                                receiver.getAddress(), receiver.getPort()
                        );
                        socket.send(packet);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Ses paketi yayınlanırken hata: " + e.getMessage());
        }
    }

    /**
     * Tüm bağlı istemcilere komut gönderir
     * @param command Gönderilecek komut
     */
    public void broadcastCommand(VoiceCommand command) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(command);

            byte[] data = baos.toByteArray();

            for (VoiceClientHandler client : connectedClients.values()) {
                DatagramPacket packet = new DatagramPacket(
                        data, data.length,
                        client.getAddress(), client.getPort()
                );
                socket.send(packet);
            }

        } catch (IOException e) {
            System.err.println("Komut yayınlanırken hata: " + e.getMessage());
        }
    }

    /**
     * Belirli bir istemciye komut gönderir
     * @param command Gönderilecek komut
     * @param targetUsername Hedef kullanıcı adı
     */
    public void sendCommandToClient(VoiceCommand command, String targetUsername) {
        VoiceClientHandler client = connectedClients.get(targetUsername);
        if (client == null) {
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(command);

            byte[] data = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(
                    data, data.length,
                    client.getAddress(), client.getPort()
            );
            socket.send(packet);

        } catch (IOException e) {
            System.err.println("Komut gönderilirken hata: " + e.getMessage());
        }
    }

    /**
     * Oyun fazı değiştiğinde, tüm istemcilere bildirim gönderir
     * @param isNight Gece fazı mı
     */
    public void notifyPhaseChange(boolean isNight) {
        VoiceCommand command = new VoiceCommand(
                VoiceCommand.CommandType.PHASE_CHANGE,
                "SERVER",
                "ALL",
                isNight ? "NIGHT" : "DAY"
        );

        broadcastCommand(command);
        voiceRoomHandler.setNightPhase(isNight);
    }

    /**
     * Oyuncu öldüğünde, ilgili istemciye bildirim gönderir
     * @param username Ölen oyuncunun kullanıcı adı
     */
    public void notifyPlayerDied(String username) {
        VoiceCommand command = new VoiceCommand(
                VoiceCommand.CommandType.PLAYER_DIED,
                username
        );

        sendCommandToClient(command, username);
        voiceRoomHandler.playerDied(username);
    }
}