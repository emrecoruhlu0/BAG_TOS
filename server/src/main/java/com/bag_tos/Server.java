package com.bag_tos;

import com.bag_tos.common.config.GameConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Town of Salem benzeri oyun için Config GUI'li sunucu uygulaması
 */
public class Server extends Application {
    private static final int PORT = 1234;
    private static final int THREAD_POOL_SIZE = 10;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private RoomHandler roomHandler;
    private ExecutorService threadPool;
    private boolean running;

    // GUI Bileşenleri
    private Button startButton;
    private Button stopButton;
    private Button restartButton;
    private Button configButton;
    private TextArea logArea;
    private Label statusLabel;
    private Label clientCountLabel;
    private Label configSummaryLabel;

    @Override
    public void start(Stage primaryStage) {
        // Sunucu nesnelerini başlat
        clients = new ArrayList<>();
        roomHandler = new RoomHandler();
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        running = false;

        // GUI oluştur
        VBox root = createGUI();

        Scene scene = new Scene(root, 700, 600);
        primaryStage.setTitle("Town of Marmara - Server (Port: " + PORT + ")");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);

        // Pencere kapatılırken sunucuyu durdur
        primaryStage.setOnCloseRequest(e -> {
            shutdown();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        // Config özetini güncelle
        updateConfigSummary();

        // Başlangıçta sunucuyu otomatik başlat
        startServer();
    }

    private VBox createGUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);

        // Başlık
        Label titleLabel = new Label("🏰 Town of Marmara Server");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Durum bilgileri
        HBox statusBox = new HBox(20);
        statusBox.setAlignment(Pos.CENTER);

        statusLabel = new Label("Durum: Durduruldu");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        clientCountLabel = new Label("Bağlı Oyuncu: 0");
        clientCountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        statusBox.getChildren().addAll(statusLabel, clientCountLabel);

        // Config özeti
        configSummaryLabel = new Label();
        configSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 5px;");
        configSummaryLabel.setWrapText(true);

        // Ana butonlar
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        startButton = new Button("▶️ Başlat");
        startButton.setPrefWidth(120);
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setOnAction(e -> startServer());

        stopButton = new Button("⏹️ Durdur");
        stopButton.setPrefWidth(120);
        stopButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        stopButton.setOnAction(e -> stopServer());
        stopButton.setDisable(true);

        restartButton = new Button("🔄 Yeniden Başlat");
        restartButton.setPrefWidth(140);
        restartButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        restartButton.setOnAction(e -> restartServer());

        configButton = new Button("⚙️ Ayarlar");
        configButton.setPrefWidth(120);
        configButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        configButton.setOnAction(e -> showConfigDialog());

        buttonBox.getChildren().addAll(startButton, stopButton, restartButton, configButton);

        // Log alanı
        Label logLabel = new Label("📋 Server Logları:");
        logLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // Alt butonlar
        HBox bottomButtons = new HBox(10);
        bottomButtons.setAlignment(Pos.CENTER);

        Button clearLogButton = new Button("🗑️ Logları Temizle");
        clearLogButton.setOnAction(e -> logArea.clear());

        Button resetConfigButton = new Button("🔧 Varsayılan Zaman");
        resetConfigButton.setOnAction(e -> {
            GameConfig.resetTimeToDefaults();
            updateConfigSummary();
            addLog("🔧 Zaman ayarları varsayılan değerlere sıfırlandı.");
        });

        bottomButtons.getChildren().addAll(clearLogButton, resetConfigButton);

        root.getChildren().addAll(
                titleLabel,
                new Separator(),
                statusBox,
                configSummaryLabel,
                buttonBox,
                new Separator(),
                logLabel,
                logArea,
                bottomButtons
        );

        return root;
    }

    private void showConfigDialog() {
        Stage configStage = new Stage();
        configStage.setTitle("⚙️ Zaman Ayarları");
        configStage.initModality(Modality.APPLICATION_MODAL);
        configStage.setResizable(false);

        VBox configRoot = new VBox(15);
        configRoot.setPadding(new Insets(20));

        // Bilgi metni
        Label infoLabel = new Label("🔧 Sadece oyun fazlarının süreleri değiştirilebilir");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        // Zaman Ayarları
        Label timeLabel = new Label("⏰ Faz Süreleri (Saniye)");
        timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(15);
        timeGrid.setVgap(10);
        timeGrid.setAlignment(Pos.CENTER);

        Spinner<Integer> nightTimeSpinner = new Spinner<>(10, 300, GameConfig.getCurrentNightDuration());
        nightTimeSpinner.setPrefWidth(80);

        Spinner<Integer> dayTimeSpinner = new Spinner<>(10, 300, GameConfig.getCurrentDayDuration());
        dayTimeSpinner.setPrefWidth(80);

        timeGrid.add(new Label("🌙 Gece Fazı:"), 0, 0);
        timeGrid.add(nightTimeSpinner, 1, 0);
        timeGrid.add(new Label("saniye"), 2, 0);

        timeGrid.add(new Label("☀️ Gündüz Fazı:"), 0, 1);
        timeGrid.add(dayTimeSpinner, 1, 1);
        timeGrid.add(new Label("saniye"), 2, 1);

        // Sabit ayarları göster (sadece bilgi için)
        Label fixedLabel = new Label("📋 Sabit Ayarlar (Değiştirilemez)");
        fixedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox fixedBox = new VBox(5);
        fixedBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10px; -fx-border-radius: 5px;");

        fixedBox.getChildren().addAll(
                new Label("👥 Min Oyuncu: " + GameConfig.MIN_PLAYERS),
                new Label("👥 Max Oyuncu: " + GameConfig.MAX_PLAYERS),
                new Label("🎭 Mafya Oranı: 1/" + GameConfig.MAX_MAFIA_RATIO),
                new Label("💀 Ölümde Rol Açık: " + (GameConfig.REVEAL_ROLES_ON_DEATH ? "Evet" : "Hayır")),
                new Label("💬 Gece Sohbet Kapalı: " + (GameConfig.DISABLE_CHAT_AT_NIGHT ? "Evet" : "Hayır"))
        );

        // Butonlar
        HBox configButtons = new HBox(10);
        configButtons.setAlignment(Pos.CENTER);

        Button saveButton = new Button("💾 Kaydet");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        saveButton.setPrefWidth(100);

        Button resetButton = new Button("🔄 Sıfırla");
        resetButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        resetButton.setPrefWidth(100);

        Button cancelButton = new Button("❌ İptal");
        cancelButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        cancelButton.setPrefWidth(100);

        configButtons.getChildren().addAll(saveButton, resetButton, cancelButton);

        // Kaydet butonu işlevi
        saveButton.setOnAction(e -> {
            try {
                GameConfig.setNightPhaseDuration(nightTimeSpinner.getValue());
                GameConfig.setDayPhaseDuration(dayTimeSpinner.getValue());

                updateConfigSummary();
                addLog("⚙️ Zaman ayarları güncellendi: Gece " + nightTimeSpinner.getValue() + "s, Gündüz " + dayTimeSpinner.getValue() + "s");
                configStage.close();

            } catch (Exception ex) {
                addLog("❌ Ayarlar kaydedilirken hata: " + ex.getMessage());
            }
        });

        // Sıfırla butonu işlevi
        resetButton.setOnAction(e -> {
            nightTimeSpinner.getValueFactory().setValue(30);
            dayTimeSpinner.getValueFactory().setValue(30);
        });

        cancelButton.setOnAction(e -> configStage.close());

        configRoot.getChildren().addAll(
                infoLabel,
                timeLabel,
                timeGrid,
                new Separator(),
                fixedLabel,
                fixedBox,
                new Separator(),
                configButtons
        );

        Scene configScene = new Scene(configRoot, 350, 400);
        configStage.setScene(configScene);
        configStage.showAndWait();
    }

    private void updateConfigSummary() {
        configSummaryLabel.setText("📊 " + GameConfig.getTimeConfigSummary());
    }

    /**
     * Sunucuyu başlatır
     */
    private void startServer() {
        if (running) {
            addLog("⚠️ Sunucu zaten çalışıyor!");
            return;
        }

        new Thread(() -> {
            try {
                // Sunucu soketi oluştur
                serverSocket = new ServerSocket(PORT);
                running = true;

                Platform.runLater(() -> {
                    statusLabel.setText("Durum: ✅ Çalışıyor");
                    statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: green; -fx-font-size: 14px;");
                    startButton.setDisable(true);
                    stopButton.setDisable(false);
                });

                addLog("🚀 Sunucu başladı. Port: " + PORT);
                addLog("📊 Ayarlar: " + GameConfig.getTimeConfigSummary());

                // Bağlantıları kabul et
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleNewConnection(clientSocket);
                    } catch (IOException e) {
                        if (running) {
                            addLog("❌ Bağlantı kabul edilirken hata: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                addLog("💥 Sunucu başlatılırken hata: " + e.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Durum: ❌ Hata");
                    statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red; -fx-font-size: 14px;");
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                });
            }
        }).start();
    }

    /**
     * Sunucuyu durdurur
     */
    private void stopServer() {
        if (!running) {
            addLog("⚠️ Sunucu zaten durdurulmuş!");
            return;
        }

        shutdown();

        Platform.runLater(() -> {
            statusLabel.setText("Durum: ⏹️ Durduruldu");
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red; -fx-font-size: 14px;");
            startButton.setDisable(false);
            stopButton.setDisable(true);
            clientCountLabel.setText("Bağlı Oyuncu: 0");
        });

        addLog("🛑 Sunucu durduruldu.");
    }

    /**
     * Sunucuyu yeniden başlatır
     */
    private void restartServer() {
        addLog("🔄 Sunucu yeniden başlatılıyor...");
        stopServer();

        // Kısa bir bekleme sonrası yeniden başlat
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 saniye bekle
                Platform.runLater(() -> startServer());
            } catch (InterruptedException e) {
                addLog("❌ Yeniden başlatma sırasında hata: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Yeni bağlantıyı işler
     */
    private void handleNewConnection(Socket clientSocket) {
        try {
            // Yeni istemci handler oluştur
            ClientHandler clientHandler = new ClientHandler(clientSocket, roomHandler);
            clients.add(clientHandler);

            // Thread havuzunda çalıştır
            threadPool.submit(clientHandler);

            Platform.runLater(() -> {
                clientCountLabel.setText("Bağlı Oyuncu: " + clients.size());
            });

            addLog("🔗 Yeni bağlantı: " + clientSocket.getInetAddress());
        } catch (IOException e) {
            addLog("❌ İstemci bağlantısı kurulurken hata: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Yok sayılabilir
            }
        }
    }

    /**
     * Sunucuyu kapatır
     */
    public void shutdown() {
        running = false;

        // İstemci bağlantılarını kapat
        for (ClientHandler client : clients) {
            try {
                client.cleanup();
            } catch (Exception e) {
                // Yok sayılabilir
            }
        }
        clients.clear();

        // Thread havuzunu kapat
        if (threadPool != null) {
            threadPool.shutdown();
        }

        // Sunucu soketini kapat
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Yok sayılabilir
            }
        }
    }

    /**
     * Log alanına mesaj ekler
     */
    private void addLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            logArea.appendText("[" + timestamp + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
        System.out.println(message); // Konsola da yazdır
    }

    /**
     * Ana metot
     */
    public static void main(String[] args) {
        launch(args);
    }
}