package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.view.LoginView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Giriş ekranını kontrol eden sınıf
 */
public class LoginController {
    private LoginView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;

    /**
     * Giriş kontrolcüsü oluşturur
     *
     * @param primaryStage Ana pencere
     * @param gameState Oyun durumu
     * @param networkManager Ağ yöneticisi
     */
    public LoginController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new LoginView();

        configureView();
    }

    /**
     * Arayüz bileşenlerini yapılandırır
     */
    private void configureView() {
        view.getConnectButton().setOnAction(e -> handleConnect());
    }

    /**
     * Bağlantı butonuna tıklandığında çağrılır
     */
    private void handleConnect() {
        String username = view.getUsernameField().getText().trim();
        String server = view.getServerField().getText().trim();
        String portText = view.getPortField().getText().trim();

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

                // UI thread'inde sonucu göster
                Platform.runLater(() -> {
                    if (connected) {
                        // Bağlantı başarılı, mesaj dinleyiciyi ayarla
                        // Ambiguous constructor hatası burada düzeltildi
                        LobbyController tempLobbyController = null;
                        MessageHandler messageHandler = new MessageHandler(tempLobbyController, gameState);
                        networkManager.setMessageListener(messageHandler);

                        // Kullanıcı adını sunucuya gönder
                        networkManager.sendMessage(username);

                        // Lobi ekranına geçiş yap
                        showLobbyScreen();
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

    /**
     * Lobi ekranına geçer
     */
    private void showLobbyScreen() {
        LobbyController lobbyController = new LobbyController(primaryStage, gameState, networkManager);
        Scene scene = new Scene(lobbyController.getView(), 800, 600);
        primaryStage.setScene(scene);
    }

    /**
     * Görünümü döndürür
     *
     * @return Giriş görünümü
     */
    public LoginView getView() {
        return view;
    }
}