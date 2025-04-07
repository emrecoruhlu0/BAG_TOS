package com.bag_tos.client.network;

import com.bag_tos.client.controller.GameController;
import com.bag_tos.client.controller.LobbyController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import javafx.application.Platform;

/**
 * Sunucudan gelen mesajları işleyip ilgili kontrolcülere yönlendiren sınıf
 */
public class MessageHandler implements NetworkManager.MessageListener {
    private GameController gameController;
    private LobbyController lobbyController;
    private GameState gameState;

    /**
     * Oyun kontrolcüsü ile mesaj işleyici oluşturur
     *
     * @param gameController Oyun kontrolcüsü
     * @param gameState Oyun durumu
     */
    public MessageHandler(GameController gameController, GameState gameState) {
        this.gameController = gameController;
        this.lobbyController = null;
        this.gameState = gameState;
    }

    /**
     * Lobi kontrolcüsü ile mesaj işleyici oluşturur
     *
     * @param lobbyController Lobi kontrolcüsü
     * @param gameState Oyun durumu
     */
    public MessageHandler(LobbyController lobbyController, GameState gameState) {
        this.lobbyController = lobbyController;
        this.gameController = null;
        this.gameState = gameState;
    }

    @Override
    public void onMessageReceived(String message) {
        // Mesaj tipine göre ayrıştırma ve işleme
        if (message.startsWith("SISTEM:")) {
            handleSystemMessage(message);
        } else if (message.startsWith("[GECE]") || message.startsWith("[GÜNDÜZ]")) {
            handlePhaseMessage(message);
        } else if (message.startsWith("AKSIYON:")) {
            handleActionMessage(message);
        } else if (message.startsWith("🔮")) {
            handleMafiaMessage(message);
        } else if (message.contains("ROL:")) {
            handleRoleAssignment(message);
        } else if (message.contains("lobiye katildi")) {
            handlePlayerJoin(message);
        } else if (message.contains("OLDURULDUN")) {
            handlePlayerDeath(message);
        } else if (message.contains("ASILDIN")) {
            handlePlayerHanged(message);
        } else if (message.contains("OYUN BİTTİ")) {
            handleGameEnd(message);
        } else {
            handleChatMessage(message);
        }

        // UI Thread'de arayüzü güncelle
        updateUI();
    }

    @Override
    public void onConnectionClosed() {
        if (gameController != null) {
            gameController.handleDisconnect();
        } else if (lobbyController != null && lobbyController.getView() != null) {
            lobbyController.handleDisconnect();
        }
    }

    /**
     * Sistem mesajlarını işler
     *
     * @param message Sistem mesajı
     */
    private void handleSystemMessage(String message) {
        gameState.addSystemMessage(message);

        if (gameController != null) {
            gameController.handleSystemMessage(message);
        } else if (lobbyController != null && lobbyController.getView() != null) {
            lobbyController.addChatMessage(message);
        }

        // Oyun başlama kontrolü
        if (message.contains("Oyun basliyor")) {
            if (lobbyController != null && lobbyController.getView() != null) {
                Platform.runLater(() -> lobbyController.startGame());
            }
        }
    }

    /**
     * Faz değişikliği mesajlarını işler
     *
     * @param message Faz mesajı
     */
    private void handlePhaseMessage(String message) {
        if (message.contains("[GECE]")) {
            gameState.setCurrentPhase(GameState.Phase.NIGHT);
        } else {
            gameState.setCurrentPhase(GameState.Phase.DAY);
        }
        gameState.addSystemMessage(message);

        if (gameController != null) {
            gameController.handleSystemMessage(message);
        }
    }

    /**
     * Aksiyon mesajlarını işler
     *
     * @param message Aksiyon mesajı
     */
    private void handleActionMessage(String message) {
        gameState.setAvailableAction(message.substring(9));
        gameState.addSystemMessage(message);

        if (gameController != null) {
            gameController.handleSystemMessage(message);
        }
    }

    /**
     * Mafya mesajlarını işler
     *
     * @param message Mafya mesajı
     */
    private void handleMafiaMessage(String message) {
        gameState.addMafiaMessage(message);

        if (gameController != null) {
            gameController.handleMafiaMessage(message);
        }
    }

    /**
     * Rol atama mesajlarını işler
     *
     * @param message Rol mesajı
     */
    private void handleRoleAssignment(String message) {
        // "ROL: Mafya" gibi mesajlardan rolü ayıkla
        if (message.contains("ROL:")) {
            String role = message.split("ROL:")[1].trim();
            gameState.setCurrentRole(role);
            gameState.addSystemMessage("Rolünüz: " + role);

            if (gameController != null) {
                gameController.handleSystemMessage("Rolünüz: " + role);
            }
        }
    }

    /**
     * Oyuncu katılımını işler
     *
     * @param message Katılım mesajı
     */
    private void handlePlayerJoin(String message) {
        // "SISTEM: username lobiye katildi! (2/4)" formatındaki mesajı işle
        try {
            String username = message.split(" ")[1];
            Player player = new Player(username);
            gameState.addPlayer(player);

            if (lobbyController != null && lobbyController.getView() != null) {
                lobbyController.updatePlayerList();
                lobbyController.addChatMessage(message);
            }
        } catch (Exception e) {
            System.err.println("Oyuncu katılımı işlenirken hata: " + e.getMessage());
        }
    }

    /**
     * Oyuncu ölümünü işler
     *
     * @param message Ölüm mesajı
     */
    private void handlePlayerDeath(String message) {
        gameState.setAlive(false);

        if (gameController != null) {
            gameController.handleSystemMessage("Öldürüldünüz!");
        }
    }

    /**
     * Oyuncu asılmasını işler
     *
     * @param message Asılma mesajı
     */
    private void handlePlayerHanged(String message) {
        gameState.setAlive(false);

        if (gameController != null) {
            gameController.handleSystemMessage("Asıldınız!");
        }
    }

    /**
     * Oyun sonu mesajını işler
     *
     * @param message Oyun sonu mesajı
     */
    private void handleGameEnd(String message) {
        if (gameController != null) {
            gameController.handleGameEnd(message);
        }
    }

    /**
     * Genel sohbet mesajlarını işler
     *
     * @param message Sohbet mesajı
     */
    private void handleChatMessage(String message) {
        gameState.addChatMessage(message);

        if (gameController != null) {
            gameController.handleChatMessage(message);
        } else if (lobbyController != null && lobbyController.getView() != null) {
            lobbyController.addChatMessage(message);
        }
    }

    /**
     * Arayüzü günceller
     */
    private void updateUI() {
        if (gameController != null) {
            gameController.updateUI();
        } else if (lobbyController != null && lobbyController.getView() != null) {
            lobbyController.updatePlayerList();
        }
    }
}