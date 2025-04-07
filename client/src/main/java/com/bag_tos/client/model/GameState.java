package com.bag_tos.client.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Oyun durumunu temsil eden model sınıfı
 */
public class GameState {
    /**
     * Oyun fazları
     */
    public enum Phase { DAY, NIGHT, LOBBY }

    private ObjectProperty<Phase> currentPhase = new SimpleObjectProperty<>(Phase.LOBBY);
    private StringProperty currentRole = new SimpleStringProperty("");
    private ObservableList<Player> players = FXCollections.observableArrayList();
    private ObservableList<String> chatMessages = FXCollections.observableArrayList();
    private ObservableList<String> mafiaMessages = FXCollections.observableArrayList();
    private ObservableList<String> systemMessages = FXCollections.observableArrayList();
    private StringProperty availableAction = new SimpleStringProperty("");
    private BooleanProperty alive = new SimpleBooleanProperty(true);
    private IntegerProperty remainingTime = new SimpleIntegerProperty(0);

    /**
     * Oyuncu ekler
     *
     * @param player Eklenecek oyuncu
     */
    public void addPlayer(Player player) {
        // Aynı kullanıcı adına sahip oyuncu varsa ekleme
        if (players.stream().noneMatch(p -> p.getUsername().equals(player.getUsername()))) {
            players.add(player);
        }
    }

    /**
     * Oyuncuyu siler
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
        players.stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .ifPresent(p -> p.setAlive(alive));
    }

    /**
     * Sohbet mesajı ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addChatMessage(String message) {
        chatMessages.add(message);
    }

    /**
     * Mafya mesajı ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addMafiaMessage(String message) {
        mafiaMessages.add(message);
    }

    /**
     * Sistem mesajı ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addSystemMessage(String message) {
        systemMessages.add(message);
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
     * Mevcut faz property'sini döndürür
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
     * Mevcut rol property'sini döndürür
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
     * Mevcut aksiyon property'sini döndürür
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
     * Hayatta olma property'sini döndürür
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
     * Kalan süre property'sini döndürür
     *
     * @return Kalan süre property'si
     */
    public IntegerProperty remainingTimeProperty() {
        return remainingTime;
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
    }
}