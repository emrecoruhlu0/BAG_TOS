package com.bag_tos.common.audio;

import java.io.Serializable;

/**
 * Ses iletişimi kontrolü için kullanılan komutları tanımlar.
 * Bu komutlar, ses verisi harici olarak gönderilen kontrol mesajlarıdır.
 */
public class VoiceCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    // Komut tipleri
    public enum CommandType {
        JOIN,           // Odaya katılma
        LEAVE,          // Odadan ayrılma
        MUTE,           // Mikrofon kapatma
        UNMUTE,         // Mikrofon açma
        HEARTBEAT,      // Bağlantı kontrolü
        PHASE_CHANGE,   // Oyun fazı değişimi (gündüz/gece)
        PLAYER_DIED,    // Oyuncu öldü
        PING,           // Gecikme ölçümü gönderimi
        PONG            // Gecikme ölçümü yanıtı
    }

    private CommandType type;       // Komut tipi
    private String username;        // Kullanıcı adı
    private String roomName;        // Oda adı
    private String extraData;       // Ek veri (gerekirse)
    private long timestamp;         // Zaman damgası

    // Boş yapıcı (Serializable için gerekli)
    public VoiceCommand() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Yeni komut oluşturur
     * @param type Komut tipi
     * @param username Kullanıcı adı
     */
    public VoiceCommand(CommandType type, String username) {
        this.type = type;
        this.username = username;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Oda bilgisi içeren komut oluşturur
     * @param type Komut tipi
     * @param username Kullanıcı adı
     * @param roomName Oda adı
     */
    public VoiceCommand(CommandType type, String username, String roomName) {
        this(type, username);
        this.roomName = roomName;
    }

    /**
     * Ek veri içeren komut oluşturur
     * @param type Komut tipi
     * @param username Kullanıcı adı
     * @param roomName Oda adı
     * @param extraData Ek veri
     */
    public VoiceCommand(CommandType type, String username, String roomName, String extraData) {
        this(type, username, roomName);
        this.extraData = extraData;
    }

    // Getter ve Setter metodları

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VoiceCommand{" +
                "type=" + type +
                ", username='" + username + '\'' +
                ", roomName='" + roomName + '\'' +
                ", extraData='" + extraData + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}