package com.bag_tos.client.model;

import javafx.beans.property.*;

/**
 * İstemci tarafında bir oyuncuyu temsil eden model sınıfı.
 * Sunucudan gelen oyuncu bilgilerinin istemci tarafında saklanmasını sağlar.
 */
public class Player {
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final BooleanProperty alive = new SimpleBooleanProperty(true);

    /**
     * Yeni bir oyuncu nesnesi oluşturur.
     */
    public Player() {
    }

    /**
     * Belirtilen kullanıcı adı ile yeni bir oyuncu nesnesi oluşturur.
     *
     * @param username Oyuncunun kullanıcı adı
     */
    public Player(String username) {
        this.username.set(username);
    }

    /**
     * Tüm bilgileri içeren bir oyuncu nesnesi oluşturur.
     *
     * @param username Oyuncunun kullanıcı adı
     * @param role Oyuncunun rolü
     * @param alive Oyuncunun hayatta olma durumu
     */
    public Player(String username, String role, boolean alive) {
        this.username.set(username);
        this.role.set(role);
        this.alive.set(alive);
    }

    /**
     * Kullanıcı adını döndürür.
     *
     * @return Kullanıcı adı
     */
    public String getUsername() {
        return username.get();
    }

    /**
     * Kullanıcı adı property'sini döndürür (UI bağlama için).
     *
     * @return Kullanıcı adı property'si
     */
    public StringProperty usernameProperty() {
        return username;
    }

    /**
     * Kullanıcı adını ayarlar.
     *
     * @param username Yeni kullanıcı adı
     */
    public void setUsername(String username) {
        this.username.set(username);
    }

    /**
     * Rolü döndürür.
     *
     * @return Rol
     */
    public String getRole() {
        return role.get();
    }

    /**
     * Rol property'sini döndürür (UI bağlama için).
     *
     * @return Rol property'si
     */
    public StringProperty roleProperty() {
        return role;
    }

    /**
     * Rolü ayarlar.
     *
     * @param role Yeni rol
     */
    public void setRole(String role) {
        this.role.set(role);
    }

    /**
     * Hayatta olma durumunu döndürür.
     *
     * @return Hayatta mı?
     */
    public boolean isAlive() {
        return alive.get();
    }

    /**
     * Hayatta olma property'sini döndürür (UI bağlama için).
     *
     * @return Hayatta olma property'si
     */
    public BooleanProperty aliveProperty() {
        return alive;
    }

    /**
     * Hayatta olma durumunu ayarlar.
     *
     * @param alive Hayatta mı?
     */
    public void setAlive(boolean alive) {
        this.alive.set(alive);
    }

    /**
     * Bu oyuncunun belirli bir roldeki mafya olup olmadığını kontrol eder.
     *
     * @return Oyuncu mafya mı?
     */
    public boolean isMafia() {
        String currentRole = getRole();
        return currentRole != null && currentRole.equals("Mafya");
    }

    /**
     * Bu oyuncunun belirli bir roldeki şerif olup olmadığını kontrol eder.
     *
     * @return Oyuncu şerif mi?
     */
    public boolean isSheriff() {
        String currentRole = getRole();
        return currentRole != null && currentRole.equals("Serif");
    }

    /**
     * Bu oyuncunun belirli bir roldeki doktor olup olmadığını kontrol eder.
     *
     * @return Oyuncu doktor mu?
     */
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