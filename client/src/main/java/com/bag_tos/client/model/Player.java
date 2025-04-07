package com.bag_tos.client.model;

import javafx.beans.property.*;

/**
 * Oyuncuyu temsil eden model sınıfı
 */
public class Player {
    private StringProperty username = new SimpleStringProperty();
    private StringProperty role = new SimpleStringProperty();
    private BooleanProperty alive = new SimpleBooleanProperty(true);

    /**
     * Oyuncu oluşturur
     *
     * @param username Kullanıcı adı
     */
    public Player(String username) {
        this.username.set(username);
    }

    /**
     * Kullanıcı adını döndürür
     *
     * @return Kullanıcı adı
     */
    public String getUsername() {
        return username.get();
    }

    /**
     * Kullanıcı adı property'sini döndürür
     *
     * @return Kullanıcı adı property'si
     */
    public StringProperty usernameProperty() {
        return username;
    }

    /**
     * Kullanıcı adını ayarlar
     *
     * @param username Kullanıcı adı
     */
    public void setUsername(String username) {
        this.username.set(username);
    }

    /**
     * Rolü döndürür
     *
     * @return Rol
     */
    public String getRole() {
        return role.get();
    }

    /**
     * Rolü ayarlar
     *
     * @param role Rol
     */
    public void setRole(String role) {
        this.role.set(role);
    }

    /**
     * Rol property'sini döndürür
     *
     * @return Rol property'si
     */
    public StringProperty roleProperty() {
        return role;
    }

    /**
     * Hayatta olma durumunu döndürür
     *
     * @return Hayatta mı?
     */
    public boolean isAlive() {
        return alive.get();
    }

    /**
     * Hayatta olma durumunu ayarlar
     *
     * @param alive Hayatta mı?
     */
    public void setAlive(boolean alive) {
        this.alive.set(alive);
    }

    /**
     * Hayatta olma property'sini döndürür
     *
     * @return Hayatta olma property'si
     */
    public BooleanProperty aliveProperty() {
        return alive;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Player player = (Player) obj;
        return getUsername().equals(player.getUsername());
    }

    @Override
    public int hashCode() {
        return getUsername().hashCode();
    }

    @Override
    public String toString() {
        return getUsername();
    }
}