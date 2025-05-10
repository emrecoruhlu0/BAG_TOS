package com.bag_tos.client.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * İstemci tarafında oyun durumunu temsil eden model sınıfı.
 * Sunucudan gelen verilerin yerel bir kopyasını tutar ve UI bileşenlerinin güncellemelerine kaynak sağlar.
 */
public class GameState {

    /**
     * Oyun fazları
     */
    public enum Phase {
        DAY,   // Gündüz fazı
        NIGHT, // Gece fazı
        LOBBY  // Lobi fazı
    }

    // --- UI'ı otomatik güncellemek için property'ler ---
    private ObjectProperty<Phase> currentPhase = new SimpleObjectProperty<>(Phase.LOBBY);
    private StringProperty currentRole = new SimpleStringProperty("");
    private ObservableList<Player> players = FXCollections.observableArrayList();
    private ObservableList<String> chatMessages = FXCollections.observableArrayList();
    private ObservableList<String> mafiaMessages = FXCollections.observableArrayList();
    private ObservableList<String> systemMessages = FXCollections.observableArrayList();
    private StringProperty availableAction = new SimpleStringProperty("");
    private BooleanProperty alive = new SimpleBooleanProperty(true);
    private IntegerProperty remainingTime = new SimpleIntegerProperty(0);

    // Sunucudan gelen ek bilgileri saklamak için genel bir yapı
    private Map<String, Object> clientData = new HashMap<>();

    /**
     * Oyuncu ekler veya günceller
     *
     * @param player Eklenecek/güncellenecek oyuncu
     */
    public void addOrUpdatePlayer(Player player) {
        if (player == null) return;

        // Önce mevcut oyuncuyu bul
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUsername().equals(player.getUsername())) {
                // Oyuncu zaten var, bilgilerini güncelle
                Player existingPlayer = players.get(i);
                existingPlayer.setAlive(player.isAlive());

                // Rol bilgisi sadece "UNKNOWN" değilse güncelle
                if (player.getRole() != null && !player.getRole().equals("UNKNOWN")) {
                    existingPlayer.setRole(player.getRole());
                }
                return;
            }
        }

        // Oyuncu bulunamadı, yeni ekle
        players.add(player);
    }

    /**
     * Oyuncu ekler (eski API uyumluluğu için)
     * Bu metot artık addOrUpdatePlayer() metoduna yönlendiriliyor.
     *
     * @param player Eklenecek oyuncu
     * @see #addOrUpdatePlayer(Player)
     */
    public void addPlayer(Player player) {
        addOrUpdatePlayer(player);
    }

    /**
     * Oyuncu siler
     *
     * @param username Silinecek oyuncunun kullanıcı adı
     */
    public void removePlayer(String username) {
        players.removeIf(player -> player.getUsername().equals(username));
    }

    /**
     * Oyuncunun durumunu günceller
     *
     * @param username Güncellenecek oyuncunun kullanıcı adı
     * @param alive Hayatta mı?
     */
    public void updatePlayerStatus(String username, boolean alive) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                player.setAlive(alive);
                break;
            }
        }
    }

    /**
     * Belirtilen kullanıcı adına sahip oyuncu nesnesini döndürür
     *
     * @param username Oyuncu kullanıcı adı
     * @return Oyuncu nesnesi, bulunamazsa null
     */
    public Player getPlayerByUsername(String username) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Genel sohbet mesajı ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addChatMessage(String message) {
        chatMessages.add(message);

        // Mesaj listesinin boyutu çok büyükse, eski mesajları temizle
        if (chatMessages.size() > 100) {
            chatMessages.remove(0, chatMessages.size() - 100);
        }
    }

    /**
     * Mafya sohbet mesajı ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addMafiaMessage(String message) {
        mafiaMessages.add(message);

        // Mesaj listesinin boyutu çok büyükse, eski mesajları temizle
        if (mafiaMessages.size() > 100) {
            mafiaMessages.remove(0, mafiaMessages.size() - 100);
        }
    }

    /**
     * Sistem mesajı ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addSystemMessage(String message) {
        systemMessages.add(message);

        // Mesaj listesinin boyutu çok büyükse, eski mesajları temizle
        if (systemMessages.size() > 100) {
            systemMessages.remove(0, systemMessages.size() - 100);
        }
    }

    /**
     * Mevcut fazı ayarlar
     *
     * @param phase Yeni faz
     */
    public void setCurrentPhase(Phase phase) {
        currentPhase.set(phase);
    }

    /**
     * Mevcut fazı döndürür
     *
     * @return Mevcut faz
     */
    public Phase getCurrentPhase() {
        return currentPhase.get();
    }

    /**
     * Mevcut faz property'sini döndürür (UI bağlama için)
     *
     * @return Mevcut faz property'si
     */
    public ObjectProperty<Phase> currentPhaseProperty() {
        return currentPhase;
    }

    /**
     * Mevcut rolü ayarlar
     *
     * @param role Yeni rol
     */
    public void setCurrentRole(String role) {
        currentRole.set(role);
    }

    /**
     * Mevcut rolü döndürür
     *
     * @return Mevcut rol
     */
    public String getCurrentRole() {
        return currentRole.get();
    }

    /**
     * Mevcut rol property'sini döndürür (UI bağlama için)
     *
     * @return Mevcut rol property'si
     */
    public StringProperty currentRoleProperty() {
        return currentRole;
    }

    /**
     * Oyuncular listesini döndürür
     *
     * @return Oyuncular listesi
     */
    public ObservableList<Player> getPlayers() {
        return players;
    }

    /**
     * Sohbet mesajları listesini döndürür
     *
     * @return Sohbet mesajları listesi
     */
    public ObservableList<String> getChatMessages() {
        return chatMessages;
    }

    /**
     * Mafya mesajları listesini döndürür
     *
     * @return Mafya mesajları listesi
     */
    public ObservableList<String> getMafiaMessages() {
        return mafiaMessages;
    }

    /**
     * Sistem mesajları listesini döndürür
     *
     * @return Sistem mesajları listesi
     */
    public ObservableList<String> getSystemMessages() {
        return systemMessages;
    }

    /**
     * Mevcut aksiyonu ayarlar
     *
     * @param action Yeni aksiyon
     */
    public void setAvailableAction(String action) {
        availableAction.set(action);
    }

    /**
     * Mevcut aksiyonu döndürür
     *
     * @return Mevcut aksiyon
     */
    public String getAvailableAction() {
        return availableAction.get();
    }

    /**
     * Mevcut aksiyon property'sini döndürür (UI bağlama için)
     *
     * @return Mevcut aksiyon property'si
     */
    public StringProperty availableActionProperty() {
        return availableAction;
    }

    /**
     * Hayatta olma durumunu ayarlar
     *
     * @param isAlive Hayatta mı?
     */
    public void setAlive(boolean isAlive) {
        alive.set(isAlive);
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
     * Hayatta olma property'sini döndürür (UI bağlama için)
     *
     * @return Hayatta olma property'si
     */
    public BooleanProperty aliveProperty() {
        return alive;
    }

    /**
     * Kalan süreyi ayarlar
     *
     * @param seconds Kalan saniye
     */
    public void setRemainingTime(int seconds) {
        remainingTime.set(seconds);
    }

    /**
     * Kalan süreyi döndürür
     *
     * @return Kalan saniye
     */
    public int getRemainingTime() {
        return remainingTime.get();
    }

    /**
     * Kalan süre property'sini döndürür (UI bağlama için)
     *
     * @return Kalan süre property'si
     */
    public IntegerProperty remainingTimeProperty() {
        return remainingTime;
    }

    /**
     * Genel client verisi ekler
     *
     * @param key Veri anahtarı
     * @param value Veri değeri
     */
    public void setData(String key, Object value) {
        clientData.put(key, value);
    }

    /**
     * Genel client verisini döndürür
     *
     * @param key Veri anahtarı
     * @return Veri değeri, yoksa null
     */
    public Object getData(String key) {
        return clientData.get(key);
    }

    /**
     * Tüm verileri sıfırlar
     */
    public void reset() {
        currentPhase.set(Phase.LOBBY);
        currentRole.set("");
        players.clear();
        chatMessages.clear();
        mafiaMessages.clear();
        systemMessages.clear();
        availableAction.set("");
        alive.set(true);
        remainingTime.set(0);
        clientData.clear();
    }
}