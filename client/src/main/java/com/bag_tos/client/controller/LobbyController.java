package com.bag_tos.client.controller;

import com.bag_tos.client.ClientApplication;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.util.AlertUtils;
import com.bag_tos.client.view.GameView;
import com.bag_tos.client.view.LobbyView;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.ChatRequest;
import com.bag_tos.common.message.request.ReadyRequest;
import com.bag_tos.common.message.request.StartGameRequest;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
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

            customImageFadeTransition(gameController.getView(), "/images/logo.png");
        });
    }

    private void customImageFadeTransition(Parent nextScreenRoot, String imagePath) {
        // Mevcut sahneyi al
        Scene currentScene = primaryStage.getScene();
        double sceneWidth = currentScene.getWidth();
        double sceneHeight = currentScene.getHeight();

        try {
            // Geçiş için görseli hazırla
            Image transitionImage = new Image(getClass().getResourceAsStream(imagePath));
            ImageView imageView = new ImageView(transitionImage);

            // Görsel boyutlarını ayarla (ekranın %80'i kadar olsun)
            double maxWidth = sceneWidth * 0.8;
            double maxHeight = sceneHeight * 0.8;

            if (transitionImage.getWidth() > maxWidth || transitionImage.getHeight() > maxHeight) {
                double scale = Math.min(maxWidth / transitionImage.getWidth(),
                        maxHeight / transitionImage.getHeight());
                imageView.setFitWidth(transitionImage.getWidth() * scale);
                imageView.setFitHeight(transitionImage.getHeight() * scale);
            }

            // Stack panel üzerinde görsel ve yeni ekranı gösterelim
            StackPane transitionPane = new StackPane();
            transitionPane.getChildren().addAll(nextScreenRoot, imageView);

            // Başlangıçta ekran ve görsel görünmez olsun
            nextScreenRoot.setOpacity(0);
            imageView.setOpacity(0);

            // Yeni sahneyi ayarla - ClientApplication'dan boyutları kullan
            Scene nextScene = new Scene(transitionPane, sceneWidth, sceneHeight);
            if (currentScene.getStylesheets().size() > 0) {
                nextScene.getStylesheets().addAll(currentScene.getStylesheets());
            }
            primaryStage.setScene(nextScene);

            // Animasyon sıralaması oluştur
            SequentialTransition sequence = new SequentialTransition();

            // 1. Adım: Görseli yavaşça göster
            FadeTransition fadeInImage = new FadeTransition(Duration.millis(200), imageView);
            fadeInImage.setFromValue(0.0);
            fadeInImage.setToValue(1.0);

            // 2. Adım: Biraz bekle
            PauseTransition pause = new PauseTransition(Duration.millis(300));

            // 3. Adım: Görseli yavaşça gizle, yeni ekranı göster
            ParallelTransition crossFade = new ParallelTransition();

            FadeTransition fadeOutImage = new FadeTransition(Duration.millis(300), imageView);
            fadeOutImage.setFromValue(1.0);
            fadeOutImage.setToValue(0.0);

            FadeTransition fadeInScreen = new FadeTransition(Duration.millis(100), nextScreenRoot);
            fadeInScreen.setFromValue(0.0);
            fadeInScreen.setToValue(1.0);

            crossFade.getChildren().addAll(fadeOutImage, fadeInScreen);

            // Tüm animasyonları sırasıyla ekle
            sequence.getChildren().addAll(fadeInImage, pause, crossFade);

            // Animasyon bitince görseli kaldır
            sequence.setOnFinished(event -> {
                transitionPane.getChildren().remove(imageView);
                centerStageOnScreen(); // Pencereyi merkeze yerleştir
            });

            // Animasyonu başlat
            sequence.play();

        } catch (Exception e) {
            System.err.println("Geçiş görseli yüklenirken hata: " + e.getMessage());
            e.printStackTrace();

            // Hata durumunda normal fade geçişi uygula - ClientApplication'dan boyutları kullan
            Scene nextScene = new Scene(nextScreenRoot, sceneWidth, sceneHeight);
            if (currentScene.getStylesheets().size() > 0) {
                nextScene.getStylesheets().addAll(currentScene.getStylesheets());
            }

            nextScreenRoot.setOpacity(0);
            primaryStage.setScene(nextScene);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), nextScreenRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    private void centerStageOnScreen() {
        // Ekran boyutlarını al
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

        // Pencere boyutlarını al
        double windowWidth = primaryStage.getWidth();
        double windowHeight = primaryStage.getHeight();

        // Merkezi konumu hesapla
        double x = (screenBounds.getWidth() - windowWidth) / 2;
        double y = (screenBounds.getHeight() - windowHeight) / 2;

        // Konumu ayarla
        primaryStage.setX(x);
        primaryStage.setY(y);
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