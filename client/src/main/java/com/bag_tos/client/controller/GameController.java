package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.util.AlertUtils;
import com.bag_tos.client.view.GameView;
import com.bag_tos.common.config.GameConfig;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.ActionRequest;
import com.bag_tos.common.message.request.ChatRequest;
import com.bag_tos.common.message.request.VoteRequest;
import com.bag_tos.common.model.ActionType;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;

public class GameController {
    private GameView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;

    public GameController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new GameView();

        configureView();
        updateUI();

        // MessageHandler'a bu kontrolcüyü bağla
        MessageHandler messageHandler = new MessageHandler(this, gameState);
        networkManager.setMessageListener(messageHandler);
    }

    private void configureView() {
        // Genel sohbet mesajı gönderme
        view.getChatPanel().setOnSendMessage(message -> {
            if (!message.isEmpty()) {
                // Gece fazında genel sohbeti devre dışı bırak
                if (gameState.getCurrentPhase() == GameState.Phase.NIGHT &&
                        GameConfig.DISABLE_CHAT_AT_NIGHT) {
                    view.addSystemMessage("Gece fazında genel sohbette konuşamazsınız!");
                    return;
                }

                // Sohbet mesajı için Message nesnesi oluştur
                Message chatMessage = new Message(MessageType.CHAT);
                ChatRequest chatRequest = new ChatRequest(message, "LOBBY");
                chatMessage.addData("chatRequest", chatRequest);

                networkManager.sendMessage(chatMessage);
            }
        });

        // Mafya sohbet mesajı gönderme
        view.getMafiaChatPanel().setOnSendMessage(message -> {
            if (!message.isEmpty() && gameState.getCurrentRole().equals("Mafya")) {
                // Mafya sohbet mesajı için Message nesnesi oluştur
                Message chatMessage = new Message(MessageType.CHAT);
                ChatRequest chatRequest = new ChatRequest(message, "MAFIA");
                chatMessage.addData("chatRequest", chatRequest);

                networkManager.sendMessage(chatMessage);
            } else {
                view.addSystemMessage("Mafya sohbetini kullanma yetkiniz yok!");
            }
        });

        // Oyuncu seçimi
        view.getPlayerListView().setOnPlayerSelected(player -> {
            System.out.println("Oyuncu seçildi: " + player.getUsername());
        });

        // Aksiyonları yapılandır
        setupActionHandlers();
    }

    private void setupActionHandlers() {
        view.getActionPanel().clearActions();

        System.out.println("setupActionHandlers çağrıldı");
        System.out.println("Faz: " + gameState.getCurrentPhase() + ", Rol: " + gameState.getCurrentRole());
        System.out.println("Hedef sayısı: " + view.getActionPanel().getTargetCount());

        // Rol ve faza göre aksiyonları yapılandır
        String role = gameState.getCurrentRole();
        GameState.Phase phase = gameState.getCurrentPhase();

        if (phase == GameState.Phase.NIGHT) {
            if (role.equals("Mafya")) {
                // Mafya öldürme aksiyonu
                view.getActionPanel().addKillAction(target -> {
                    // Kendi üzerinde aksiyon kontrolü
                    if (!GameConfig.ALLOW_SELF_ACTIONS &&
                            target.getUsername().equals(gameState.getCurrentUsername())) {
                        view.addSystemMessage("Kendiniz üzerinde aksiyon yapamazsınız!");
                        return;
                    }

                    // Öldürme aksiyonu için Message nesnesi oluştur
                    Message actionMessage = new Message(MessageType.ACTION);
                    ActionRequest actionRequest = new ActionRequest(ActionType.KILL.name(), target.getUsername());
                    actionMessage.addData("actionRequest", actionRequest);

                    networkManager.sendMessage(actionMessage);
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            } else if (role.equals("Doktor")) {
                // Doktor iyileştirme aksiyonu
                view.getActionPanel().addHealAction(target -> {

                    // İyileştirme aksiyonu için Message nesnesi oluştur
                    Message actionMessage = new Message(MessageType.ACTION);
                    ActionRequest actionRequest = new ActionRequest(ActionType.HEAL.name(), target.getUsername());
                    actionMessage.addData("actionRequest", actionRequest);

                    networkManager.sendMessage(actionMessage);
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            } else if (role.equals("Serif")) {
                // Şerif araştırma aksiyonu
                view.getActionPanel().addInvestigateAction(target -> {
                    // Kendi üzerinde aksiyon kontrolü
                    if (!GameConfig.ALLOW_SELF_ACTIONS &&
                            target.getUsername().equals(gameState.getCurrentUsername())) {
                        view.addSystemMessage("Kendiniz üzerinde aksiyon yapamazsınız!");
                        return;
                    }

                    // Araştırma aksiyonu için Message nesnesi oluştur
                    Message actionMessage = new Message(MessageType.ACTION);
                    ActionRequest actionRequest = new ActionRequest(ActionType.INVESTIGATE.name(), target.getUsername());
                    actionMessage.addData("actionRequest", actionRequest);

                    networkManager.sendMessage(actionMessage);
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            }
        } else if (phase == GameState.Phase.DAY) {
            // Gündüz fazında oylamaya izin ver
            view.getActionPanel().addVoteAction(target -> {
                // Kendi kendine oy kontrolü
                if (!GameConfig.ALLOW_SELF_ACTIONS &&
                        target.getUsername().equals(gameState.getCurrentUsername())) {
                    view.addSystemMessage("Kendinize oy veremezsiniz!");
                    return;
                }

                // Oylama aksiyonu için Message nesnesi oluştur
                Message voteMessage = new Message(MessageType.VOTE);
                VoteRequest voteRequest = new VoteRequest(target.getUsername());
                voteMessage.addData("voteRequest", voteRequest);

                networkManager.sendMessage(voteMessage);
                view.addSystemMessage("Oy verildi: " + target.getUsername());
            });
        }
    }

    // MessageHandler tarafından kullanılacak metot (sunucudan gelen aksiyonlara göre UI'ı günceller)
    public void updateActions(List<String> availableActions) {
        Platform.runLater(() -> {
            view.getActionPanel().clearActions();

            // Geçerli oyuncu bilgileri
            String currentUsername = gameState.getCurrentUsername();
            String currentRole = gameState.getCurrentRole();

            System.out.println("Aksiyon güncelleniyor - Rol: " + currentRole + ", Kullanıcı: " + currentUsername);

            // Doktor için tüm canlı oyuncular (kendisi dahil)
            if (currentRole != null && currentRole.equals("Doktor")) {
                System.out.println("Doktor rolü için tüm canlı oyuncular hedef listesine ekleniyor (kendisi dahil)");
                view.getActionPanel().setAlivePlayers(gameState.getPlayers());
            } else {
                // Diğer roller için kendisi hariç oyuncular
                System.out.println("Diğer roller için canlı oyuncular hedef listesine ekleniyor (kendisi hariç)");
                view.getActionPanel().setAlivePlayers(gameState.getPlayers(), currentUsername);
            }

            System.out.println("Toplam hedef sayısı: " + view.getActionPanel().getTargetCount());

            if (availableActions != null) {
                for (String action : availableActions) {
                    switch (action) {
                        case "KILL":
                            view.getActionPanel().addKillAction(target -> {
                                Message actionMessage = new Message(MessageType.ACTION);
                                ActionRequest actionRequest = new ActionRequest(ActionType.KILL.name(), target.getUsername());
                                actionMessage.addData("actionRequest", actionRequest);

                                networkManager.sendMessage(actionMessage);
                                view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                            });
                            break;
                        case "HEAL":
                            view.getActionPanel().addHealAction(target -> {
                                // Doktor rolü için özel durum ekle
                                if (!GameConfig.ALLOW_SELF_ACTIONS &&
                                        target.getUsername().equals(gameState.getCurrentUsername()) &&
                                        !"Doktor".equals(gameState.getCurrentRole())) {
                                    // Doktor DEĞİLSE kendine aksiyon yapma
                                    view.addSystemMessage("Kendiniz üzerinde aksiyon yapamazsınız!");
                                    return;
                                }

                                Message actionMessage = new Message(MessageType.ACTION);
                                ActionRequest actionRequest = new ActionRequest(ActionType.HEAL.name(), target.getUsername());
                                actionMessage.addData("actionRequest", actionRequest);

                                networkManager.sendMessage(actionMessage);
                                view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                            });
                            break;
                        case "INVESTIGATE":
                            view.getActionPanel().addInvestigateAction(target -> {
                                Message actionMessage = new Message(MessageType.ACTION);
                                ActionRequest actionRequest = new ActionRequest(ActionType.INVESTIGATE.name(), target.getUsername());
                                actionMessage.addData("actionRequest", actionRequest);

                                networkManager.sendMessage(actionMessage);
                                view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                            });
                            break;
                        case "VOTE":
                            view.getActionPanel().addVoteAction(target -> {
                                Message voteMessage = new Message(MessageType.VOTE);
                                VoteRequest voteRequest = new VoteRequest(target.getUsername());
                                voteMessage.addData("voteRequest", voteRequest);

                                networkManager.sendMessage(voteMessage);
                                view.addSystemMessage("Oy verildi: " + target.getUsername());
                            });
                            break;
                        case "JAIL":
                            view.getActionPanel().addJailAction(target -> {
                                Message actionMessage = new Message(MessageType.ACTION);
                                ActionRequest actionRequest = new ActionRequest(ActionType.JAIL.name(), target.getUsername());
                                actionMessage.addData("actionRequest", actionRequest);

                                networkManager.sendMessage(actionMessage);
                                view.addSystemMessage("Hapsedilecek hedef seçildi: " + target.getUsername());
                            });
                            break;
                        case "EXECUTE":
                            view.getActionPanel().addExecuteAction(target -> {
                                Message actionMessage = new Message(MessageType.ACTION);
                                ActionRequest actionRequest = new ActionRequest(ActionType.EXECUTE.name(), target.getUsername());
                                actionMessage.addData("actionRequest", actionRequest);

                                networkManager.sendMessage(actionMessage);
                                view.addSystemMessage("İnfaz hedefi seçildi: " + target.getUsername());
                            });
                            break;
                    }
                }
            }
        });
    }
    public void updateUI() {
        Platform.runLater(() -> {
            updatePhaseDisplay();
            updateRoleDisplay();
            updatePlayerListDisplay();
            updateTimeDisplay();
            updateActionControls();
            updateChatControls();

            // Oyuncu adını güncelle
            view.updateUsername(gameState.getCurrentUsername());
        });
    }

    // Her bileşen için ayrı güncelleme metodları
    private void updatePhaseDisplay() {
        view.updatePhase(gameState.getCurrentPhase());
        System.out.println("UI Faz Güncellemesi: " + gameState.getCurrentPhase()); // Debug log
    }

    private void updateRoleDisplay() {
        view.updateRole(gameState.getCurrentRole());
    }

    private void updatePlayerListDisplay() {
        view.getPlayerListView().updatePlayers(gameState.getPlayers());
    }

    private void updateTimeDisplay() {
        view.updateTime(gameState.getRemainingTime());
    }

    private void updateActionControls() {
        // Aksiyon panelini güncelle
        System.out.println("Aksiyon Kontrollerini Güncelleme - Faz: " + gameState.getCurrentPhase()); // Debug log
        setupActionHandlers();
    }

    private void updateChatControls() {
        // Gece/gündüz durumuna göre sohbet kontrollerini güncelle
        GameState.Phase currentPhase = gameState.getCurrentPhase();
        boolean isNight = gameState.getCurrentPhase() == GameState.Phase.NIGHT;

        // Debug log
        System.out.println("Chat kontrollerini güncelleme - Faz: " + currentPhase);

        // Gece fazında genel sohbeti devre dışı bırak
        if (isNight && GameConfig.DISABLE_CHAT_AT_NIGHT) {
            view.getChatPanel().getMessageField().setDisable(true);
            view.getChatPanel().getSendButton().setDisable(true);
            System.out.println("Gece fazı - sohbet devre dışı"); // Debug log
        } else {
            view.getChatPanel().getMessageField().setDisable(false);
            view.getChatPanel().getSendButton().setDisable(false);
            System.out.println("Gündüz fazı - sohbet etkin"); // Debug log
        }

        // Mafya sohbeti güncelleme
        boolean isMafia = gameState.getCurrentRole().equals("Mafya");
        view.getMafiaChatPanel().getMessageField().setDisable(!isMafia);
        view.getMafiaChatPanel().getSendButton().setDisable(!isMafia);

        // Ölü oyuncu kontrolü
        if (!gameState.isAlive() && !GameConfig.ALLOW_DEAD_CHAT) {
            view.getChatPanel().getMessageField().setDisable(true);
            view.getChatPanel().getSendButton().setDisable(true);
            view.getMafiaChatPanel().getMessageField().setDisable(true);
            view.getMafiaChatPanel().getSendButton().setDisable(true);
        }
    }

    /**
     * Sadece süre göstergesini günceller
     */
    public void updateTimeOnly() {
        Platform.runLater(() -> {
            // Sadece kalan zamanı güncelle, diğer UI öğeleri aynı kalır
            view.updateTime(gameState.getRemainingTime());
        });
    }

    /**
     * Sadece oyuncu listesini günceller
     */
    public void updatePlayerListOnly() {
        Platform.runLater(() -> {
            // Sadece oyuncu listesini güncelle
            view.getPlayerListView().updatePlayers(gameState.getPlayers());
        });
    }

    public void updateActionsOnly() {
        Platform.runLater(() -> {
            // Debug log
            System.out.println("updateActionsOnly çağrıldı");
            System.out.println("Oyuncu sayısı: " + gameState.getPlayers().size());

            // Aksiyon panelini temizle
            view.getActionPanel().clearActions();

            // Önce oyuncu listesini ayarla
            String currentRole = gameState.getCurrentRole();
            String currentUsername = gameState.getCurrentUsername();

            System.out.println("Güncel rol: " + currentRole + ", Kullanıcı: " + currentUsername);

            // Doktor rolü için özel durum kontrolü
            if ("Doktor".equals(currentRole)) {
                // Doktor tüm oyuncuları görebilir (kendisi dahil)
                view.getActionPanel().setAlivePlayers(gameState.getPlayers());
            } else {
                // Diğer roller kendilerini göremez
                view.getActionPanel().setAlivePlayers(gameState.getPlayers(), currentUsername);
            }

            // Sonra aksiyonları yapılandır
            setupActionHandlers();
        });
    }

    public void handleSystemMessage(String message) {
        Platform.runLater(() -> {
            view.addSystemMessage(message);
        });
    }

    public void handleChatMessage(String message) {
        Platform.runLater(() -> {
            view.addChatMessage(message);
        });
    }

    public void handleMafiaMessage(String message) {
        Platform.runLater(() -> {
            view.addMafiaMessage(message);
        });
    }

    public void handleGameEnd(String winnerMessage) {
        Platform.runLater(() -> {
            // Oyun sonu mesajını göster
            Alert alert = AlertUtils.createAlert(
                    Alert.AlertType.INFORMATION,
                    primaryStage,
                    "Oyun Bitti",
                    "Oyun Sonucu",
                    winnerMessage
            );
            alert.showAndWait();

            // Lobiye geri dön
            LobbyController lobbyController = new LobbyController(primaryStage, new GameState(), networkManager);
            Scene scene = new Scene(lobbyController.getView(), 800, 600);

            // CSS stillerini ekle
            if (getClass().getResource("/css/application.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            }

            primaryStage.setScene(scene);
        });
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
            Scene scene = new Scene(loginController.getView(), 600, 400);
            primaryStage.setScene(scene);
        });
    }

    public GameView getView() {
        return view;
    }
}