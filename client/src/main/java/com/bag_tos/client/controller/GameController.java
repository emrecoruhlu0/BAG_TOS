package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.MessageHandler;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.util.AlertUtils;
import com.bag_tos.client.view.GameView;
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

        // Rol ve faza göre aksiyonları yapılandır
        String role = gameState.getCurrentRole();
        GameState.Phase phase = gameState.getCurrentPhase();

        if (phase == GameState.Phase.NIGHT) {
            if (role.equals("Mafya")) {
                // Mafya öldürme aksiyonu
                view.getActionPanel().addKillAction(target -> {
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
            view.getActionPanel().setAlivePlayers(gameState.getPlayers());

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
                    }
                }
            }
        });
    }

    public void updateUI() {
        Platform.runLater(() -> {
            // Faz bilgisini güncelle
            view.updatePhase(gameState.getCurrentPhase());

            // Rol bilgisini güncelle
            view.updateRole(gameState.getCurrentRole());

            // Oyuncu listesini güncelle
            view.getPlayerListView().updatePlayers(gameState.getPlayers());

            // Kalan zamanı güncelle
            view.updateTime(gameState.getRemainingTime());

            // Aksiyonları güncelle (temel aksiyon yapılandırması)
            setupActionHandlers();

            // Mafya sekmesini role göre etkinleştir/devre dışı bırak
            if (gameState.getCurrentRole().equals("Mafya")) {
                // Mafya sekmesini etkinleştir
                view.getMafiaChatPanel().getMessageField().setDisable(false);
                view.getMafiaChatPanel().getSendButton().setDisable(false);
            } else {
                // Mafya sekmesini devre dışı bırak
                view.getMafiaChatPanel().getMessageField().setDisable(true);
                view.getMafiaChatPanel().getSendButton().setDisable(true);
            }

            // Oyuncu ölüyse aksiyonları devre dışı bırak
            if (!gameState.isAlive()) {
                view.getActionPanel().clearActions();
                view.getChatPanel().getMessageField().setDisable(true);
                view.getChatPanel().getSendButton().setDisable(true);
                view.getMafiaChatPanel().getMessageField().setDisable(true);
                view.getMafiaChatPanel().getSendButton().setDisable(true);
                view.addSystemMessage("Ölü olduğunuz için aksiyon yapamazsınız!");
            }
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