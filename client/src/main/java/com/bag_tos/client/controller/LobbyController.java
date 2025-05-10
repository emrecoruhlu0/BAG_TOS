package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.util.AlertUtils;
import com.bag_tos.client.view.LobbyView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;

/**
 * Lobi ekranını kontrol eden sınıf
 */
public class LobbyController {
    private LobbyView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;

    /**
     * Lobi kontrolcüsü oluşturur
     *
     * @param primaryStage Ana pencere
     * @param gameState Oyun durumu
     * @param networkManager Ağ yöneticisi
     */
    public LobbyController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new LobbyView();

        // MessageHandler'a bu kontrolcüyü bağla
        MessageHandler messageHandler = new MessageHandler(this, gameState);
        networkManager.setMessageListener(messageHandler);

        configureView();
    }

    /**
     * Arayüz bileşenlerini yapılandırır
     */
    private void configureView() {
        // Hazır butonu
        view.getReadyButton().setOnAction(e -> {
            networkManager.sendMessage("/ready");
            view.getReadyButton().setDisable(true);
            view.addChatMessage("Hazır oldunuz. Diğer oyuncular bekleniyor...");
        });

        // Başlat butonu
        view.getStartButton().setOnAction(e -> {
            networkManager.sendMessage("/start");
            view.getStartButton().setDisable(true);
            view.addChatMessage("Oyun başlatma isteği gönderildi...");
        });

        // Mesaj gönderme
        view.getSendButton().setOnAction(e -> sendChatMessage());
        view.getMessageField().setOnAction(e -> sendChatMessage());
    }

    /**
     * Sohbet mesajı gönderir
     */
    private void sendChatMessage() {
        String message = view.getMessageField().getText().trim();
        if (!message.isEmpty()) {
            networkManager.sendMessage(message);
            view.getMessageField().clear();
        }
    }

    /**
     * Oyuncu listesini günceller
     */
    public void updatePlayerList() {
        Platform.runLater(() -> {
            // GameState'den oyuncuları al
            List<Player> currentPlayers = gameState.getPlayers();
            System.out.println("updatePlayerList çağrıldı, oyuncu sayısı: " + currentPlayers.size());

            // LobbyView'a aktar
            view.updatePlayerList(currentPlayers);
        });
    }

    /**
     * Sohbet alanına mesaj ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addChatMessage(String message) {
        Platform.runLater(() -> {
            view.addChatMessage(message);
        });
    }

    /**
     * Oyun ekranına geçer
     */
    public void startGame() {
        Platform.runLater(() -> {
            GameController gameController = new GameController(primaryStage, gameState, networkManager);
            Scene scene = new Scene(gameController.getView(), 900, 700);
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
     * @return Lobi görünümü
     */
    public LobbyView getView() {
        return view;
    }
}