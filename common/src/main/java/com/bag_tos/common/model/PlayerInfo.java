package com.bag_tos.common.model;

/**
 * Oyuncu bilgilerini tutan model sınıfı
 */
public class PlayerInfo {
    private String username;
    private boolean alive;
    private String role;  // İstemciye gönderilirken diğer oyuncular için "UNKNOWN" olabilir

    // Boş constructor
    public PlayerInfo() {
    }

    // Constructor
    public PlayerInfo(String username, boolean alive, String role) {
        this.username = username;
        this.alive = alive;
        this.role = role;
    }

    // Getter ve Setter metodları
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}