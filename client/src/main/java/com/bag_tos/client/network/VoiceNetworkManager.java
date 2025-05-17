package com.bag_tos.client.network;

import com.bag_tos.common.audio.AudioFormat;
import com.bag_tos.common.audio.VoiceCommand;
import com.bag_tos.common.audio.VoiceCommand.CommandType;
import com.bag_tos.common.audio.VoicePacket;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ses verisini ağ üzerinden gönderme ve alma işlemlerini yönetir.
 * UDP protokolü kullanarak ses paketlerini iletir.
 */
public class VoiceNetworkManager {
    private String serverAddress;
    private int serverPort;
    private String username;
    private DatagramSocket socket;

    // Bağlantı durumu
    private AtomicBoolean connected = new AtomicBoolean(false);

    // Paket sayacı
    private AtomicInteger packetCounter = new AtomicInteger(0);

    // Dinleyiciler
    private VoicePacketListener packetListener;
    private VoiceCommandListener commandListener;

    /**
     * Ses ağ yöneticisi oluşturur
     */
    public VoiceNetworkManager() {
    }

    /**
     * Sunucuya bağlanır
     * @param serverAddress Sunucu adresi
     * @param serverPort Sunucu portu
     * @param username Kullanıcı adı
     * @return Bağlantı başarılı ise true
     */
    public boolean connect(String serverAddress, int serverPort, String username) {
        try {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.username = username;

            // UDP soketi oluştur
            socket = new DatagramSocket();

            // Bağlantı durumunu güncelle
            connected.set(true);

            // Paket dinleme thread'i başlat
            startListening();

            // LOBBY odasına katılma mesajı gönder
            sendJoinCommand("LOBBY");

            System.out.println("Ses sunucusuna bağlandı: " + serverAddress + ":" + serverPort);
            return true;

        } catch (SocketException e) {
            System.err.println("Ses bağlantısı kurulurken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sunucudan gelen paketleri dinler
     */
    private void startListening() {
        Thread listenerThread = new Thread(this::listenForPackets);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Paket dinleme döngüsü
     */
    private void listenForPackets() {
        byte[] buffer = new byte[AudioFormat.BUFFER_SIZE];

        while (connected.get() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Paketi işle
                processReceivedPacket(packet);

            } catch (IOException e) {
                if (connected.get()) {
                    System.err.println("Paket alınırken hata: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Alınan paketi işler
     * @param packet Alınan UDP paketi
     */
    private void processReceivedPacket(DatagramPacket packet) {
        try {
            // Veriyi deserialize et
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);

            Object receivedObject = ois.readObject();

            if (receivedObject instanceof VoicePacket) {
                // Ses paketi
                VoicePacket voicePacket = (VoicePacket) receivedObject;

                // Dinleyici varsa bildirimi gönder
                if (packetListener != null) {
                    packetListener.onVoicePacketReceived(voicePacket);
                }

            } else if (receivedObject instanceof VoiceCommand) {
                // Komut paketi
                VoiceCommand command = (VoiceCommand) receivedObject;

                // Dinleyici varsa bildirimi gönder
                if (commandListener != null) {
                    commandListener.onVoiceCommandReceived(command);
                }

                // PONG komutu için gecikme hesaplama
                if (command.getType() == CommandType.PONG) {
                    calculateLatency(command.getTimestamp());
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Paket işlenirken hata: " + e.getMessage());
        }
    }

    /**
     * Ping-Pong ile gecikme ölçümü
     * @param pingTimestamp Gönderim zamanı
     */
    private void calculateLatency(long pingTimestamp) {
        long currentTime = System.currentTimeMillis();
        long latency = currentTime - pingTimestamp;
        System.out.println("Ses gecikme süresi: " + latency + "ms");
    }

    /**
     * Ses paketi gönderir
     * @param audioData Ses verisi
     * @param isSilence Sessizlik paketi mi
     * @return Gönderim başarılı ise true
     */
    public boolean sendVoicePacket(byte[] audioData, boolean isSilence) {
        if (!connected.get() || socket.isClosed()) {
            return false;
        }

        try {
            // Ses paketi oluştur
            VoicePacket voicePacket = new VoicePacket(
                    username,
                    "LOBBY", // Sadece LOBBY odasına ses gönderme
                    packetCounter.incrementAndGet(),
                    audioData,
                    isSilence
            );

            // Paketi serialize et
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(voicePacket);

            byte[] data = baos.toByteArray();

            // Sunucuya gönder
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
            socket.send(packet);

            return true;

        } catch (IOException e) {
            System.err.println("Ses paketi gönderilirken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Komut gönderir
     * @param command Gönderilecek komut
     * @return Gönderim başarılı ise true
     */
    public boolean sendCommand(VoiceCommand command) {
        if (!connected.get() || socket.isClosed()) {
            return false;
        }

        try {
            // Paketi serialize et
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(command);

            byte[] data = baos.toByteArray();

            // Sunucuya gönder
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
            socket.send(packet);

            return true;

        } catch (IOException e) {
            System.err.println("Komut gönderilirken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Odaya katılma komutu gönderir
     * @param roomName Oda adı
     * @return Gönderim başarılı ise true
     */
    public boolean sendJoinCommand(String roomName) {
        VoiceCommand command = new VoiceCommand(CommandType.JOIN, username, roomName);
        return sendCommand(command);
    }

    /**
     * Odadan ayrılma komutu gönderir
     * @param roomName Oda adı
     * @return Gönderim başarılı ise true
     */
    public boolean sendLeaveCommand(String roomName) {
        VoiceCommand command = new VoiceCommand(CommandType.LEAVE, username, roomName);
        return sendCommand(command);
    }

    /**
     * Mikrofon kapatma komutu gönderir
     * @return Gönderim başarılı ise true
     */
    public boolean sendMuteCommand() {
        VoiceCommand command = new VoiceCommand(CommandType.MUTE, username);
        return sendCommand(command);
    }

    /**
     * Mikrofon açma komutu gönderir
     * @return Gönderim başarılı ise true
     */
    public boolean sendUnmuteCommand() {
        VoiceCommand command = new VoiceCommand(CommandType.UNMUTE, username);
        return sendCommand(command);
    }

    /**
     * Gecikme ölçümü için PING gönderir
     * @return Gönderim başarılı ise true
     */
    public boolean sendPingCommand() {
        VoiceCommand command = new VoiceCommand(CommandType.PING, username);
        return sendCommand(command);
    }

    /**
     * Bağlantıyı kapatır
     */
    public void disconnect() {
        if (connected.getAndSet(false)) {
            // LOBBY odasından ayrıl
            sendLeaveCommand("LOBBY");

            // Soket kapat
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            System.out.println("Ses sunucusu bağlantısı kapatıldı");
        }
    }

    // Getter ve Setter metodları

    public boolean isConnected() {
        return connected.get();
    }

    public void setPacketListener(VoicePacketListener listener) {
        this.packetListener = listener;
    }

    public void setCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }

    /**
     * Ses paketi alma olayı dinleyicisi
     */
    public interface VoicePacketListener {
        void onVoicePacketReceived(VoicePacket packet);
    }

    /**
     * Komut alma olayı dinleyicisi
     */
    public interface VoiceCommandListener {
        void onVoiceCommandReceived(VoiceCommand command);
    }

    public String getUsername() {
        return username;
    }
}