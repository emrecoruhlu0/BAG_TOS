package com.bag_tos.client;

import com.bag_tos.client.controller.LoginController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.NetworkManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    private GameState gameState;
    private NetworkManager networkManager;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Model ve ağ bağlantısı oluştur
            gameState = new GameState();
            networkManager = new NetworkManager();

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double screenWidth = screenBounds.getWidth();
            double screenHeight = screenBounds.getHeight();

            // Ekranın %80'i kadar
            double width = screenWidth * 0.8;
            double height = screenHeight * 0.8;

            // Ana pencere ayarları
            primaryStage.setTitle("Town of Marmara");
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Opsiyonel: Uygulama ikonu
            try {
                if (getClass().getResource("/images/logo.png") != null) {
                    primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
                }
            } catch (Exception e) {
                System.err.println("Logo yüklenirken hata: " + e.getMessage());
            }

            LoginController loginController = new LoginController(primaryStage, gameState, networkManager);
            primaryStage.show();

            // Pencere kapatılırken doğru şekilde temizlensin
            primaryStage.setOnCloseRequest(event -> {
                stop();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Uygulama kapandığında bağlantıyı kapat
        if (networkManager != null) {
            networkManager.disconnect();
            networkManager.shutdown();
        }

        // JVM'i düzgünce sonlandır
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}