package com.bag_tos.client.controller;

import com.bag_tos.client.ClientApplication;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.view.LoginView;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;

public class LoginController {
    private LoginView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;

    public LoginController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new LoginView();

        setupWindow();
        //setupResponsiveLayout();
        configureView();
    }

    private void setupWindow() {
        // Pencere boyutunu ve başlığını ayarla
        primaryStage.setTitle("Town of Salem Clone - Login");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Başlangıç boyutu (daha büyük)
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);

        // Sahneyi oluştur ve ayarla
        Scene scene = new Scene(view);
        primaryStage.setScene(scene);

        // Pencereyi ekranın ortasına yerleştir
        primaryStage.centerOnScreen();

        // CSS stil dosyasını ekle
        if (getClass().getResource("/css/application.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        }
    }


    private void configureView() {
        view.getConnectButton().setOnAction(e -> handleConnect());
    }

    private void handleConnect() {
        String username = view.getUsernameField().getText().trim();
        String server = view.getServerField().getText().trim();
        String portText = view.getPortField().getText().trim();
        String avatarId = view.getSelectedAvatarId();

        if (username.isEmpty()) {
            view.setStatusText("Lütfen kullanıcı adı giriniz!");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            view.setStatusText("Sunucuya bağlanılıyor...");
            view.getConnectButton().setDisable(true);

            // Arka planda bağlantı işlemini yap
            new Thread(() -> {
                boolean connected = networkManager.connect(server, port);

                Platform.runLater(() -> {
                    if (connected) {
                        // Kullanıcı adını ve avatar ID'sini sunucuya gönder
                        Message authMessage = new Message(MessageType.READY);
                        authMessage.addData("username", username);
                        authMessage.addData("avatarId", avatarId);
                        networkManager.sendMessage(authMessage);

                        // GameState'e bilgileri kaydet
                        gameState.setCurrentUsername(username);

                        // Oyuncu nesnesini oluştur ve GameState'e ekle
                        Player currentPlayer = new Player(username);
                        currentPlayer.setAvatarId(avatarId);
                        gameState.addOrUpdatePlayer(currentPlayer);

                        // Lobi ekranına geçiş yap ve LobbyController oluştur
                        LobbyController lobbyController = new LobbyController(primaryStage, gameState, networkManager);

                        // MessageHandler'ı yapılandır
                        MessageHandler messageHandler = new MessageHandler(lobbyController, gameState);
                        networkManager.setMessageListener(messageHandler);

                        // Animasyonlu geçiş kullan
                        customImageFadeTransition(lobbyController.getView(), "/images/logo.png");
                    } else {
                        view.setStatusText("Bağlantı hatası! Lütfen tekrar deneyin.");
                        view.getConnectButton().setDisable(false);
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            view.setStatusText("Geçerli bir port numarası giriniz!");
        }
    }

    private void customImageFadeTransition(Parent nextScreenRoot, String imagePath) {
        // Mevcut sahneyi al
        Scene currentScene = primaryStage.getScene();
        double sceneWidth = currentScene.getWidth();
        double sceneHeight = currentScene.getHeight();

                try {
                    // Geçiş görselini hazırla
                    InputStream imageStream = getClass().getResourceAsStream(imagePath);

                    // Görsel yoksa basit geçiş yap
                    if (imageStream == null) {
                        System.err.println("UYARI: Görsel bulunamadı: " + imagePath);

                        // Basit fade geçişi - tam ekran boyutlarını kullan
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
                        return;
                    }

                    // Görsel bulundu
                    Image transitionImage = new Image(imageStream);
                    ImageView imageView = new ImageView(transitionImage);

                    // Görsel boyutunu tam ekrana göre ayarla
                    double maxWidth = sceneWidth * 0.8;
                    double maxHeight = sceneHeight * 0.8;

                    if (transitionImage.getWidth() > maxWidth || transitionImage.getHeight() > maxHeight) {
                        double scale = Math.min(maxWidth / transitionImage.getWidth(),
                                maxHeight / transitionImage.getHeight());
                        imageView.setFitWidth(transitionImage.getWidth() * scale);
                        imageView.setFitHeight(transitionImage.getHeight() * scale);
                    }

                    // Stack panel oluştur - tam ekran boyutlarını kullan
                    StackPane transitionPane = new StackPane();
                    transitionPane.getChildren().addAll(nextScreenRoot, imageView);

                    // Başlangıçta görünürlükleri ayarla
                    nextScreenRoot.setOpacity(0);
                    imageView.setOpacity(0);

                    // Tam ekran boyutlarıyla yeni sahne oluştur
                    Scene nextScene = new Scene(transitionPane, sceneWidth, sceneHeight);
                    if (currentScene.getStylesheets().size() > 0) {
                        nextScene.getStylesheets().addAll(currentScene.getStylesheets());
                    }
                    primaryStage.setScene(nextScene);

                    // Animasyon sıralaması
                    SequentialTransition sequence = new SequentialTransition();

                    // 1. Adım: Görseli yavaşça göster
                    FadeTransition fadeInImage = new FadeTransition(Duration.millis(300), imageView);
                    fadeInImage.setFromValue(0.0);
                    fadeInImage.setToValue(1.0);

                    // 2. Adım: Biraz bekle
                    PauseTransition pause = new PauseTransition(Duration.millis(400));

                    // 3. Adım: Görseli gizle, yeni ekranı göster
                    ParallelTransition crossFade = new ParallelTransition();

                    FadeTransition fadeOutImage = new FadeTransition(Duration.millis(300), imageView);
                    fadeOutImage.setFromValue(1.0);
                    fadeOutImage.setToValue(0.0);

                    FadeTransition fadeInScreen = new FadeTransition(Duration.millis(300), nextScreenRoot);
                    fadeInScreen.setFromValue(0.0);
                    fadeInScreen.setToValue(1.0);

                    crossFade.getChildren().addAll(fadeOutImage, fadeInScreen);

                    // Animasyonları sırayla ekle
                    sequence.getChildren().addAll(fadeInImage, pause, crossFade);

            // Animasyon bitince görseli kaldır
            sequence.setOnFinished(event -> {
                transitionPane.getChildren().remove(imageView);
            });

                    // Animasyonu başlat
                    sequence.play();

                } catch (Exception ex) {
                    System.err.println("Geçiş işlemi sırasında hata: " + ex.getMessage());
                    ex.printStackTrace();

                    // Hata durumunda basit geçiş yap
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

    public LoginView getView() {
        return view;
    }
}