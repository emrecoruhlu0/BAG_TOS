package com.bag_tos.client;

import com.bag_tos.client.controller.LoginController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.NetworkManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Town of Salem benzeri oyun için JavaFX tabanlı istemci uygulaması
 */
public class ClientApplication extends Application {
    private GameState gameState;
    private NetworkManager networkManager;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Model ve ağ bağlantısı oluştur
            gameState = new GameState();
            networkManager = new NetworkManager();

            // Ana pencere ayarları
            primaryStage.setTitle("Town of Salem Clone");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Opsiyonel: Uygulama ikonu
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));

            // İlk ekranı göster (Login)
            LoginController loginController = new LoginController(primaryStage, gameState, networkManager);
            Scene scene = new Scene(loginController.getView(), 600, 400);

            // CSS stil dosyasını ekle (opsiyonel)
            // scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Uygulama kapandığında bağlantıyı kapat
        if (networkManager != null) {
            networkManager.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}