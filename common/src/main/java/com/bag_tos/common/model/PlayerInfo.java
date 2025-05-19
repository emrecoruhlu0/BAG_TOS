package com.bag_tos.common.model;

public class PlayerInfo {
    private String username;
    private boolean alive;
    private String role;
    private String avatarId;

    public PlayerInfo() {
    }

    public PlayerInfo(String username, boolean alive, String role) {
        this.username = username;
        this.alive = alive;
        this.role = role;
    }

    public PlayerInfo(String username, boolean alive, String role, String avatarId) {
        this.username = username;
        this.alive = alive;
        this.role = role;
        this.avatarId = avatarId;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
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