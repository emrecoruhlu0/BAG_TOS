package com.bag_tos.client;

import com.bag_tos.client.controller.LoginController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.NetworkManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

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
                // Önce .ico dosyasını dene
                InputStream iconStream = getClass().getResourceAsStream("/images/app-icon.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                    System.out.println("ICO ikon yüklendi");
                } else {
                    // .ico yoksa .png kullan
                    iconStream = getClass().getResourceAsStream("/images/app-icon.png");
                    if (iconStream != null) {
                        primaryStage.getIcons().add(new Image(iconStream));
                        System.out.println("PNG ikon yüklendi");
                    } else {
                        System.err.println("Hiçbir ikon dosyası bulunamadı");
                    }
                }
            } catch (Exception e) {
                System.err.println("İkon yüklenirken hata: " + e.getMessage());
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