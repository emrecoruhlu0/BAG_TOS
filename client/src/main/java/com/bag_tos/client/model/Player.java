package com.bag_tos.client.model;

import javafx.beans.property.*;

public class Player {
    // Mevcut özellikler
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final BooleanProperty alive = new SimpleBooleanProperty(true);

    // Yeni avatar özelliği
    private final StringProperty avatarId = new SimpleStringProperty("avatar1"); // Varsayılan

    // Yapıcılar
    public Player() {
    }

    public Player(String username) {
        this.username.set(username);
    }

    public Player(String username, String role, boolean alive) {
        this.username.set(username);
        this.role.set(role);
        this.alive.set(alive);
    }

    public Player(String username, String role, boolean alive, String avatarId) {
        this.username.set(username);
        this.role.set(role);
        this.alive.set(alive);
        this.avatarId.set(avatarId);
    }

    public String getAvatarId() {
        return avatarId.get();
    }

    public StringProperty avatarIdProperty() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId.set(avatarId);
    }

    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public String getRole() {
        return role.get();
    }

    public StringProperty roleProperty() {
        return role;
    }

    public void setRole(String role) {
        String oldRole = this.role.get();
        this.role.set(role);
        System.out.println("DEBUG: Oyuncu rol değişikliği - Oyuncu: " + getUsername() +
                ", Eski Rol: " + oldRole + ", Yeni Rol: " + role);
    }

    public boolean isAlive() {
        return alive.get();
    }

    public BooleanProperty aliveProperty() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive.set(alive);
    }

    public boolean isMafia() {
        String currentRole = getRole();
        return currentRole != null && currentRole.equals("Mafya");
    }

    public boolean isSheriff() {
        String currentRole = getRole();
        return currentRole != null && currentRole.equals("Serif");
    }

    public boolean isDoctor() {
        String currentRole = getRole();
        return currentRole != null && currentRole.equals("Doktor");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Player player = (Player) obj;
        return getUsername() != null && getUsername().equals(player.getUsername());
    }

    @Override
    public int hashCode() {
        return getUsername() != null ? getUsername().hashCode() : 0;
    }

    @Override
    public String toString() {
        return getUsername();
    }
}