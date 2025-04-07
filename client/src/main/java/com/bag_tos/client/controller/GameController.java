package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.util.AlertUtils;
import com.bag_tos.client.view.GameView;
import com.bag_tos.client.view.components.ActionPanel;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Oyun ekranını kontrol eden sınıf
 */
public class GameController {
    private GameView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;

    /**
     * Game controller oluşturur
     *
     * @param primaryStage Ana pencere
     * @param gameState Oyun durumu
     * @param networkManager Ağ yöneticisi
     */
    public GameController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new GameView();

        // MessageHandler'a bu kontrolcüyü bağla
        MessageHandler messageHandler = new MessageHandler(this, gameState);
        networkManager.setMessageListener(messageHandler);

        configureView();
        updateUI();
    }

    /**
     * Arayüz bileşenlerini yapılandırır
     */
    private void configureView() {
        // Mesaj gönderme
        view.getChatPanel().setOnSendMessage(message -> {
            if (message.startsWith("/")) {
                // Komut gönderimi
                networkManager.sendMessage(message);
            } else {
                // Normal mesaj
                networkManager.sendMessage(message);
            }
        });

        // Oyuncu seçimi
        view.getPlayerListView().setOnPlayerSelected(player -> {
            // Seçilen oyuncu ile bir şey yap (opsiyonel)
            System.out.println("Oyuncu seçildi: " + player.getUsername());
        });

        // Aksiyon paneli işleyicileri
        setupActionHandlers();
    }

    /**
     * Aksiyon paneli işleyicilerini ayarlar
     */
    private void setupActionHandlers() {
        // Öldürme aksiyonu işleyicisi
        view.getActionPanel().addKillAction(target -> {
            networkManager.sendMessage("/oldur " + target.getUsername());
            view.addSystemMessage("Hedef seçildi: " + target.getUsername());
        });

        // İyileştirme aksiyonu işleyicisi
        view.getActionPanel().addHealAction(target -> {
            networkManager.sendMessage("/iyilestir " + target.getUsername());
            view.addSystemMessage("Hedef seçildi: " + target.getUsername());
        });

        // Oylama aksiyonu işleyicisi
        view.getActionPanel().addVoteAction(target -> {
            networkManager.sendMessage("/oyla " + target.getUsername());
            view.addSystemMessage("Oy verildi: " + target.getUsername());
        });
    }

    /**
     * Arayüzü oyun durumuna göre günceller
     */
    public void updateUI() {
        Platform.runLater(() -> {
            // Faz bilgisini güncelle
            view.updatePhase(gameState.getCurrentPhase());

            // Rol bilgisini güncelle
            view.updateRole(gameState.getCurrentRole());

            // Oyuncu listesini güncelle
            view.getPlayerListView().updatePlayers(gameState.getPlayers());

            // Aksiyonları güncelle
            updateActions();
        });
    }

    /**
     * Aksiyonları oyuncunun rolüne ve oyun fazına göre günceller
     */
    private void updateActions() {
        view.getActionPanel().clearActions();
        view.getActionPanel().setAlivePlayers(gameState.getPlayers());

        String role = gameState.getCurrentRole();
        GameState.Phase phase = gameState.getCurrentPhase();

        // Faza ve role göre uygun aksiyonları göster
        if (phase == GameState.Phase.NIGHT) {
            if (role.equals("Mafya")) {
                view.getActionPanel().addKillAction(target -> {
                    networkManager.sendMessage("/oldur " + target.getUsername());
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            } else if (role.equals("Doktor")) {
                view.getActionPanel().addHealAction(target -> {
                    networkManager.sendMessage("/iyilestir " + target.getUsername());
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            } else if (role.equals("Serif")) {
                view.getActionPanel().addInvestigateAction(target -> {
                    networkManager.sendMessage("/arastir " + target.getUsername());
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            }
        } else if (phase == GameState.Phase.DAY) {
            view.getActionPanel().addVoteAction(target -> {
                networkManager.sendMessage("/oyla " + target.getUsername());
                view.addSystemMessage("Oy verildi: " + target.getUsername());
            });
        }
    }

    /**
     * Sistem mesajını işler
     *
     * @param message Sistem mesajı
     */
    public void handleSystemMessage(String message) {
        Platform.runLater(() -> {
            view.addSystemMessage(message);
        });
    }

    /**
     * Sohbet mesajını işler
     *
     * @param message Sohbet mesajı
     */
    public void handleChatMessage(String message) {
        Platform.runLater(() -> {
            view.addChatMessage(message);
        });
    }

    /**
     * Mafya mesajını işler
     *
     * @param message Mafya mesajı
     */
    public void handleMafiaMessage(String message) {
        Platform.runLater(() -> {
            view.addMafiaMessage(message);
        });
    }

    /**
     * Oyun sonu mesajını işler
     *
     * @param winnerMessage Kazanan mesajı
     */
    public void handleGameEnd(String winnerMessage) {
        Platform.runLater(() -> {
            // Oyun sonu mesajını göster
            Alert alert = AlertUtils.createAlert(
                    Alert.AlertType.INFORMATION,
                    primaryStage,
                    "Oyun Bitti",
                    "Oyun Sonucu",
                    winnerMessage
            );
            alert.showAndWait();

            // Lobiye geri dön
            LobbyController lobbyController = new LobbyController(primaryStage, new GameState(), networkManager);
            Scene scene = new Scene(lobbyController.getView(), 800, 600);
            primaryStage.setScene(scene);
        });
    }

    /**
     * Bağlantı kopmasını işler
     */
    public void handleDisconnect() {
        Platform.runLater(() -> {
            Alert alert = AlertUtils.createAlert(
                    Alert.AlertType.ERROR,
                    primaryStage,
                    "Bağlantı Hatası",
                    null,
                    "Sunucuyla bağlantı kesildi!"
            );
            alert.showAndWait();

            // Login ekranına dön
            LoginController loginController = new LoginController(primaryStage, new GameState(), new NetworkManager());
            Scene scene = new Scene(loginController.getView(), 600, 400);
            primaryStage.setScene(scene);
        });
    }

    /**
     * Görünümü döndürür
     *
     * @return Oyun görünümü
     */
    public GameView getView() {
        return view;
    }
}