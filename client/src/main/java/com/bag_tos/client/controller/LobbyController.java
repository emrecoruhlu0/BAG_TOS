package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.util.AlertUtils;
import com.bag_tos.client.view.LobbyView;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.ChatRequest;
import com.bag_tos.common.message.request.ReadyRequest;
import com.bag_tos.common.message.request.StartGameRequest;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;

public class LobbyController {
    private LobbyView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;
    private boolean isReady = false;

    public LobbyController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new LobbyView();

        configureView();
    }

    private void configureView() {
        // Hazır butonu
        view.getReadyButton().setOnAction(e -> {
            if (!isReady) {
                // Message nesnesi oluştur ve gönder
                Message readyMessage = new Message(MessageType.READY);
                ReadyRequest readyRequest = new ReadyRequest(true);
                readyMessage.addData("readyRequest", readyRequest);

                networkManager.sendMessage(readyMessage);

                view.getReadyButton().setText("Hazır ✓");
                view.getReadyButton().setDisable(true);
                isReady = true;

                addChatMessage("Hazır oldunuz. Diğer oyuncular bekleniyor...");
            }
        });

        // Başlat butonu
        view.getStartButton().setOnAction(e -> {
            // Message nesnesi oluştur ve gönder
            Message startMessage = new Message(MessageType.START_GAME);
            StartGameRequest startRequest = new StartGameRequest();
            startMessage.addData("startGameRequest", startRequest);

            networkManager.sendMessage(startMessage);

            view.getStartButton().setDisable(true);

            addChatMessage("Oyun başlatma isteği gönderildi...");
        });

        // Mesaj gönderme
        view.getChatPanel().setOnSendMessage(message -> {
            if (!message.isEmpty()) {
                // Sohbet mesajı için Message nesnesi oluştur
                Message chatMessage = new Message(MessageType.CHAT);
                ChatRequest chatRequest = new ChatRequest(message, "LOBBY");
                chatMessage.addData("chatRequest", chatRequest);

                networkManager.sendMessage(chatMessage);
            }
        });
    }

    public void updatePlayerList() {
        Platform.runLater(() -> {
            // GameState'den oyuncuları al
            List<Player> currentPlayers = gameState.getPlayers();

            // LobbyView'a aktar
            view.updatePlayerList(currentPlayers);

            // Hazır sayısını güncelle (eğer GameState'de böyle bir veri varsa)
            Integer readyCount = (Integer) gameState.getData("readyCount");
            if (readyCount != null) {
                view.updateReadyCount(readyCount);
            }
        });
    }

    public void addChatMessage(String message) {
        Platform.runLater(() -> {
            view.addChatMessage(message);
        });
    }

    public void startGame() {
        Platform.runLater(() -> {
            GameController gameController = new GameController(primaryStage, gameState, networkManager);

            // Mevcut oyuncuların avatar bilgilerini GameController'a aktar
            for (Player player : gameState.getPlayers()) {
                gameController.setSelectedAvatar(player.getUsername(), player.getAvatarId());
            }

            Scene scene = new Scene(gameController.getView(), 900, 700);

            // CSS stillerini ekle
            if (getClass().getResource("/css/application.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            }

            primaryStage.setScene(scene);
        });
    }

    public void handleGameStateUpdate(Message gameStateMessage) {
        // Oyun durumu güncelleme
        String state = (String) gameStateMessage.getDataValue("state");
        if ("GAME_STARTING".equals(state)) {
            // Oyun başlıyor, oyun ekranına geç
            startGame();
        }

        // Hazır sayısını güncelle
        Integer readyCount = (Integer) gameStateMessage.getDataValue("readyCount");
        if (readyCount != null) {
            gameState.setData("readyCount", readyCount);
            Platform.runLater(() -> {
                view.updateReadyCount(readyCount);
            });
        }
    }

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
            Scene scene = new Scene(loginController.getView(), 500, 350);
            primaryStage.setScene(scene);
        });
    }

    public LobbyView getView() {
        return view;
    }
}