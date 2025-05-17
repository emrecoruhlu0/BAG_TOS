package com.bag_tos.common.audio;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Ağ üzerinden iletilen ses paketleri için veri yapısı.
 * Bu sınıf, ses verisinin yanı sıra, kimlik doğrulama ve yönlendirme
 * için gerekli meta verileri de içerir.
 */
public class VoicePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    // Paket meta verileri
    private String username;        // Gönderen kullanıcı adı
    private String roomName;        // Hedef oda adı (LOBBY, MAFIA, vs.)
    private long timestamp;         // Zaman damgası (gönderim sırası)
    private int packetId;           // Paket sıra numarası
    private boolean isSilence;      // Sessizlik paketi mi?

    // Asıl ses verisi
    private byte[] audioData;       // Sıkıştırılmamış ses verisi

    // Boş yapıcı (Serializable için gerekli)
    public VoicePacket() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Yeni ses paketi oluşturur
     * @param username Gönderen kullanıcı adı
     * @param roomName Hedef oda adı
     * @param packetId Paket sıra numarası
     * @param audioData Ses verisi
     * @param isSilence Sessizlik paketi mi
     */
    public VoicePacket(String username, String roomName, int packetId, byte[] audioData, boolean isSilence) {
        this.username = username;
        this.roomName = roomName;
        this.packetId = packetId;
        this.audioData = audioData;
        this.isSilence = isSilence;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Bu paketin oda filtrelerine göre hedef odaya gitmesi gerekip gerekmediğini kontrol eder
     * @param targetRoom Kontrol edilecek hedef oda
     * @return Paket bu odaya gidecekse true
     */
    public boolean isForRoom(String targetRoom) {
        // Oda null ise, tüm odalara gider
        if (roomName == null) {
            return true;
        }

        // Odalar eşleşiyor mu
        return roomName.equals(targetRoom);
    }

    // Getter ve Setter metodları

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    public boolean isSilence() {
        return isSilence;
    }

    public void setSilence(boolean silence) {
        isSilence = silence;
    }

    @Override
    public String toString() {
        return "VoicePacket{" +
                "username='" + username + '\'' +
                ", roomName='" + roomName + '\'' +
                ", timestamp=" + timestamp +
                ", packetId=" + packetId +
                ", isSilence=" + isSilence +
                ", audioDataLength=" + (audioData != null ? audioData.length : 0) +
                '}';
    }
}