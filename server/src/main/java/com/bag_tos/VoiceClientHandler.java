package com.bag_tos;

import java.net.InetAddress;

/**
 * Bir ses istemcisinin bağlantı bilgilerini tutar.
 * UDP bağlantılarında, istemcilerin IP ve port bilgilerini izlemek gerekir.
 */
public class VoiceClientHandler {
    private final String username;
    private InetAddress address;
    private int port;
    private long lastActivity;
    private boolean muted;
    private boolean alive;

    /**
     * Yeni istemci oluşturur
     * @param username Kullanıcı adı
     * @param address IP adresi
     * @param port Port numarası
     */
    public VoiceClientHandler(String username, InetAddress address, int port) {
        this.username = username;
        this.address = address;
        this.port = port;
        this.lastActivity = System.currentTimeMillis();
        this.muted = false;
        this.alive = true;
    }

    /**
     * Bağlantı bilgilerini günceller
     * @param address Yeni IP adresi
     * @param port Yeni port numarası
     */
    public void updateConnectionInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.lastActivity = System.currentTimeMillis();
    }

    /**
     * Son aktivite zamanını günceller
     */
    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    /**
     * İstemcinin aktif olup olmadığını kontrol eder
     * @param timeoutMs Zaman aşımı (milisaniye)
     * @return İstemci aktifse true
     */
    public boolean isActive(long timeoutMs) {
        return (System.currentTimeMillis() - lastActivity) < timeoutMs;
    }

    // Getter ve Setter metodları

    public String getUsername() {
        return username;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public String toString() {
        return "VoiceClientHandler{" +
                "username='" + username + '\'' +
                ", address=" + address +
                ", port=" + port +
                ", lastActivity=" + lastActivity +
                ", muted=" + muted +
                ", alive=" + alive +
                '}';
    }
}