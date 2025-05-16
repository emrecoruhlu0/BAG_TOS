package com.bag_tos.client.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.HashMap;
import java.util.Map;

public class GameState {

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
    private String currentUsername;

    // Sunucudan gelen ek bilgileri saklamak için genel bir yapı
    private Map<String, Object> clientData = new HashMap<>();

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

    public void addPlayer(Player player) {
        addOrUpdatePlayer(player);
    }

    public void removePlayer(String username) {
        players.removeIf(player -> player.getUsername().equals(username));
    }

    public void updatePlayerStatus(String username, boolean alive) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                player.setAlive(alive);
                break;
            }
        }
    }

    public Player getPlayerByUsername(String username) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                return player;
            }
        }
        return null;
    }

    public void addChatMessage(String message) {
        chatMessages.add(message);

        // Mesaj listesinin boyutu çok büyükse, eski mesajları temizle
        if (chatMessages.size() > 100) {
            chatMessages.remove(0, chatMessages.size() - 100);
        }
    }

    public void addMafiaMessage(String message) {
        mafiaMessages.add(message);

        // Mesaj listesinin boyutu çok büyükse, eski mesajları temizle
        if (mafiaMessages.size() > 100) {
            mafiaMessages.remove(0, mafiaMessages.size() - 100);
        }
    }

    public void addSystemMessage(String message) {
        systemMessages.add(message);

        // Mesaj listesinin boyutu çok büyükse, eski mesajları temizle
        if (systemMessages.size() > 100) {
            systemMessages.remove(0, systemMessages.size() - 100);
        }
    }

    public void setCurrentPhase(Phase phase) {
        currentPhase.set(phase);
    }

    public Phase getCurrentPhase() {
        return currentPhase.get();
    }

    public ObjectProperty<Phase> currentPhaseProperty() {
        return currentPhase;
    }

    public void setCurrentRole(String role) {
        String oldRole = currentRole.get();
        currentRole.set(role);
        System.out.println("DEBUG: Mevcut rol değişti: " + oldRole + " -> " + role +
                " (Kullanıcı: " + currentUsername + ")");

        // Ayrıca bu rolü Player nesnesine de ekleyelim
        for (Player p : players) {
            if (p.getUsername().equals(currentUsername)) {
                p.setRole(role);
                System.out.println("DEBUG: Rol aynı zamanda Player nesnesine de atandı: " + currentUsername);
                break;
            }
        }
    }

    public String getCurrentRole() {
        return currentRole.get();
    }

    public StringProperty currentRoleProperty() {
        return currentRole;
    }

    public ObservableList<Player> getPlayers() {
        return players;
    }

    public ObservableList<String> getChatMessages() {
        return chatMessages;
    }

    public ObservableList<String> getMafiaMessages() {
        return mafiaMessages;
    }

    public ObservableList<String> getSystemMessages() {
        return systemMessages;
    }

    public void setAvailableAction(String action) {
        availableAction.set(action);
    }

    public String getAvailableAction() {
        return availableAction.get();
    }

    public StringProperty availableActionProperty() {
        return availableAction;
    }

    public void setAlive(boolean isAlive) {
        alive.set(isAlive);
    }

    public boolean isAlive() {
        return alive.get();
    }

    public BooleanProperty aliveProperty() {
        return alive;
    }

    public void setRemainingTime(int seconds) {
        remainingTime.set(seconds);
    }

    public int getRemainingTime() {
        return remainingTime.get();
    }

    public IntegerProperty remainingTimeProperty() {
        return remainingTime;
    }

    public void setData(String key, Object value) {
        clientData.put(key, value);
    }

    public Object getData(String key) {
        return clientData.get(key);
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

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