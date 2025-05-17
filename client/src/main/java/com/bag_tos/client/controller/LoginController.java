package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.view.LoginView;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

        configureView();
    }

    private void configureView() {
        view.getConnectButton().setOnAction(e -> handleConnect());
    }

    private void handleConnect() {
        String username = view.getUsernameField().getText().trim();
        String server = view.getServerField().getText().trim();
        String portText = view.getPortField().getText().trim();
        String avatarId = view.getSelectedAvatarId(); // Avatar bilgisini al

        System.out.println("Login ekranında seçilen avatar: " + username + " -> " + avatarId);

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
                        authMessage.addData("avatarId", avatarId); // Avatar ID'sini ekle
                        networkManager.sendMessage(authMessage);

                        // GameState'e bilgileri kaydet
                        gameState.setCurrentUsername(username);

                        // Oyuncu nesnesini oluştur ve GameState'e ekle
                        Player currentPlayer = new Player(username);
                        currentPlayer.setAvatarId(avatarId);
                        gameState.addOrUpdatePlayer(currentPlayer);

                        // Lobi ekranına geçiş yap ve LobbyController oluştur
                        LobbyController lobbyController = new LobbyController(primaryStage, gameState, networkManager);
                        lobbyController.setSelectedAvatar(username, avatarId);

                        // MessageHandler'ı yapılandır
                        MessageHandler messageHandler = new MessageHandler(lobbyController, gameState);
                        networkManager.setMessageListener(messageHandler);

                        // Yeni Scene'i göster
                        Scene scene = new Scene(lobbyController.getView(), 700, 500);

                        // CSS stil dosyasını ekle
                        if (getClass().getResource("/css/application.css") != null) {
                            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
                        }

                        primaryStage.setScene(scene);
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

    public LoginView getView() {
        return view;
    }
}