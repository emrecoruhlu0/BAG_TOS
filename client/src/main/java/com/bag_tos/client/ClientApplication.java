package com.bag_tos.client;

import com.bag_tos.client.controller.LoginController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.NetworkManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Town of Salem benzeri oyun için JavaFX tabanlı istemci uygulaması.
 * Uygulamanın başlangıç noktasıdır.
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
            primaryStage.setMinWidth(700);
            primaryStage.setMinHeight(500);

            // Opsiyonel: Uygulama ikonu
            try {
                if (getClass().getResource("/images/logo.png") != null) {
                    primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
                }
            } catch (Exception e) {
                System.err.println("Logo yüklenirken hata: " + e.getMessage());
            }

            // İlk ekranı göster (Login)
            LoginController loginController = new LoginController(primaryStage, gameState, networkManager);
            Scene scene = new Scene(loginController.getView(), 600, 400);

            // CSS stil dosyasını ekle
            try {
                if (getClass().getResource("/css/application.css") != null) {
                    scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("CSS dosyası yüklenirken hata: " + e.getMessage());
            }

            primaryStage.setScene(scene);
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

    /**
     * Ana metot
     *
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        launch(args);
    }
}