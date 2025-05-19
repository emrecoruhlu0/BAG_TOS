package com.bag_tos.client.controller;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.network.NetworkManager;
import com.bag_tos.client.view.GameView;
import com.bag_tos.common.config.GameConfig;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.ActionRequest;
import com.bag_tos.common.message.request.VoteRequest;
import com.bag_tos.common.model.ActionType;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ActionManager {
    private GameState gameState;
    private NetworkManager networkManager;
    private GameView view;

    private static long lastUpdateTime = 0;

    public ActionManager(GameState gameState, NetworkManager networkManager, GameView view) {
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = view;
    }


    public void updateActions() {
        // Eğer zaten UI thread'inde değilsek, Platform.runLater kullan
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::updateActions);
            return;
        }

        try {
            // Statik bir bayrak kullanarak son güncelleme zamanını kontrol et
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < 200) { // 200ms içinde tekrar güncelleme yapma
                System.out.println("[DEBUG] ActionManager: Çok sık güncelleme isteği, atlanıyor. Son güncellemeden beri: " +
                        (currentTime - lastUpdateTime) + "ms");
                return;
            }
            lastUpdateTime = currentTime;

            // Aksiyon panelini temizle
            view.getActionPanel().clearActions();

            // Mevcut durum bilgilerini al
            String currentRole = gameState.getCurrentRole();
            GameState.Phase currentPhase = gameState.getCurrentPhase();
            String currentUsername = gameState.getCurrentUsername();

            System.out.println("[DEBUG] ActionManager: Aksiyon güncelleniyor - Rol: " + currentRole +
                    ", Faz: " + currentPhase + ", Kullanıcı: " + currentUsername);

            // Özel durumları kontrol et
            if (handleSpecialCases()) {
                return; // Özel durum varsa metodu bitir
            }

            // Faza göre aksiyonları ekle
            if (currentPhase == GameState.Phase.NIGHT) {
                addNightActions(currentRole);
            } else if (currentPhase == GameState.Phase.DAY) {
                addDayActions(currentRole);
            }
        } catch (Exception e) {
            System.err.println("[HATA] ActionManager: Aksiyonlar güncellenirken hata - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean handleSpecialCases() {
        String currentRole = gameState.getCurrentRole();
        GameState.Phase currentPhase = gameState.getCurrentPhase();

        // Ölü oyuncu kontrolü
        if (!gameState.isAlive()) {
            System.out.println("[DEBUG] ActionManager: Oyuncu ölü, aksiyon yapamaz");
            return true;
        }

        // Hapsedilmiş oyuncu kontrolü
        Boolean isJailed = (Boolean) gameState.getData("isJailed");
        if (isJailed != null && isJailed && currentPhase == GameState.Phase.NIGHT) {
            System.out.println("[DEBUG] ActionManager: Oyuncu hapsedilmiş, aksiyon yapamaz");
            view.addSystemMessage("Hapsedildiniz, herhangi bir aksiyon gerçekleştiremezsiniz.");
            return true;
        }

        // Jester kontrolü - gece aksiyonu yok
        if ("Jester".equals(currentRole) && currentPhase == GameState.Phase.NIGHT) {
            System.out.println("[DEBUG] ActionManager: Jester rolü, gece aksiyonu yok");
            view.addSystemMessage("Jester olarak gece aksiyonunuz bulunmuyor.");
            return true;
        }

        // Pasif Jailor kontrolü
        if (gameState.getAvailableAction() != null &&
                gameState.getAvailableAction().contains("NO_ACTION") &&
                "Gardiyan".equals(currentRole) &&
                currentPhase == GameState.Phase.NIGHT) {
            System.out.println("[DEBUG] ActionManager: Pasif gardiyan, infaz yapamaz");
            showInactiveJailorMessage();
            return true;
        }

        return false;
    }

    private void showInactiveJailorMessage() {
        VBox actionPanel = view.getActionPanel();
        actionPanel.getChildren().clear();

        Label noActionLabel = new Label("Bu gece aksiyon yok - Gündüz fazında kimseyi hapsetmediniz");
        noActionLabel.getStyleClass().add("warning-text");
        actionPanel.getChildren().add(noActionLabel);

        view.addSystemMessage("Gündüz fazında kimseyi hapsetmediniz. Bu gece aksiyon gerçekleştiremeyeceksiniz.");
    }

    private void addNightActions(String role) {
        if (role == null) {
            System.out.println("[HATA] ActionManager: Rol bilgisi bulunamadı!");
            return;
        }

        switch (role) {
            case "Mafya":
                addMafiaKillAction();
                break;
            case "Doktor":
                addDoctorHealAction();
                break;
            case "Serif":
                addSheriffInvestigateAction();
                break;
            case "Gardiyan":
                addJailorExecuteAction();
                break;
            default:
                System.out.println("[DEBUG] ActionManager: Bilinmeyen rol: " + role);
                break;
        }
    }

    private void addDayActions(String role) {
        // Tüm oyuncular için oylama aksiyonu
        addVotingAction();

        // Gardiyan rolü için hapsetme aksiyonu
        if ("Gardiyan".equals(role)) {
            addJailorJailAction();
        }
    }

    private void addMafiaKillAction() {
        System.out.println("[DEBUG] ActionManager: Mafya öldürme aksiyonu ekleniyor...");

        // Mafya üyelerini topla
        List<String> mafiaUsernames = new ArrayList<>();
        for (Player p : gameState.getPlayers()) {
            if ("Mafya".equals(p.getRole())) {
                mafiaUsernames.add(p.getUsername());
                System.out.println("[DEBUG] ActionManager: Mafya üyesi: " + p.getUsername());
            }
        }

        // Sadece hayatta olan ve mafya olmayan oyuncuları filtrele
        List<Player> validTargets = gameState.getPlayers().stream()
                .filter(p -> p.isAlive() && !mafiaUsernames.contains(p.getUsername()))
                .collect(Collectors.toList());

        System.out.println("[DEBUG] ActionManager: Filtrelenen hedef sayısı: " + validTargets.size());

        // Hedef listesi boşsa işlem yapma
        if (validTargets.isEmpty()) {
            view.addSystemMessage("Hedef alınabilecek oyuncu kalmadı.");
            return;
        }

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Öldür:");

        // String tabanlı ComboBox oluştur
        ComboBox<String> targetCombo = new ComboBox<>();
        for (Player p : validTargets) {
            targetCombo.getItems().add(p.getUsername());
        }
        targetCombo.setPromptText("Hedef seçin");

        Button actionButton = new Button("Öldür");
        actionButton.getStyleClass().add("danger-button");
        actionButton.setOnAction(e -> {
            String selectedUsername = targetCombo.getValue();
            if (selectedUsername != null) {
                // Aksiyon mesajı oluştur
                Message actionMessage = new Message(MessageType.ACTION);
                ActionRequest actionRequest = new ActionRequest(ActionType.KILL.name(), selectedUsername);
                actionMessage.addData("actionRequest", actionRequest);

                // Mesajı gönder
                networkManager.sendMessage(actionMessage);
                view.addSystemMessage("Hedef seçildi: " + selectedUsername);
                targetCombo.setValue(null);
            }
        });

        actionBox.getChildren().addAll(actionLabel, targetCombo, actionButton);
        view.getActionPanel().getChildren().add(actionBox);

        System.out.println("[DEBUG] ActionManager: Mafya öldürme aksiyonu eklendi");
    }

    private void addDoctorHealAction() {
        System.out.println("[DEBUG] ActionManager: Doktor iyileştirme aksiyonu ekleniyor...");

        // Tüm hayatta olan oyuncuları al (doktor kendisini de iyileştirebilir)
        List<Player> validTargets = gameState.getPlayers().stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());

        // Hedef listesi boşsa işlem yapma
        if (validTargets.isEmpty()) {
            view.addSystemMessage("İyileştirilecek oyuncu kalmadı.");
            return;
        }

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("İyileştir:");

        // String tabanlı ComboBox oluştur
        ComboBox<String> targetCombo = new ComboBox<>();
        for (Player p : validTargets) {
            targetCombo.getItems().add(p.getUsername());
        }
        targetCombo.setPromptText("Hedef seçin");

        Button actionButton = new Button("İyileştir");
        actionButton.getStyleClass().add("primary-button");
        actionButton.setOnAction(e -> {
            String selectedUsername = targetCombo.getValue();
            if (selectedUsername != null) {
                // Aksiyon mesajı oluştur
                Message actionMessage = new Message(MessageType.ACTION);
                ActionRequest actionRequest = new ActionRequest(ActionType.HEAL.name(), selectedUsername);
                actionMessage.addData("actionRequest", actionRequest);

                // Mesajı gönder
                networkManager.sendMessage(actionMessage);
                view.addSystemMessage("Hedef seçildi: " + selectedUsername);
                targetCombo.setValue(null);
            }
        });

        actionBox.getChildren().addAll(actionLabel, targetCombo, actionButton);
        view.getActionPanel().getChildren().add(actionBox);

        System.out.println("[DEBUG] ActionManager: Doktor iyileştirme aksiyonu eklendi");
    }

    private void addSheriffInvestigateAction() {
        System.out.println("[DEBUG] ActionManager: Şerif araştırma aksiyonu ekleniyor...");

        // Kendisi dışındaki hayatta olan oyuncuları al
        String currentUsername = gameState.getCurrentUsername();
        List<Player> validTargets = gameState.getPlayers().stream()
                .filter(p -> p.isAlive() && !p.getUsername().equals(currentUsername))
                .collect(Collectors.toList());

        // Hedef listesi boşsa işlem yapma
        if (validTargets.isEmpty()) {
            view.addSystemMessage("Araştırılacak oyuncu kalmadı.");
            return;
        }

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Araştır:");

        // String tabanlı ComboBox oluştur
        ComboBox<String> targetCombo = new ComboBox<>();
        for (Player p : validTargets) {
            targetCombo.getItems().add(p.getUsername());
        }
        targetCombo.setPromptText("Hedef seçin");

        Button actionButton = new Button("Araştır");
        actionButton.getStyleClass().add("primary-button");
        actionButton.setOnAction(e -> {
            String selectedUsername = targetCombo.getValue();
            if (selectedUsername != null) {
                // Aksiyon mesajı oluştur
                Message actionMessage = new Message(MessageType.ACTION);
                ActionRequest actionRequest = new ActionRequest(ActionType.INVESTIGATE.name(), selectedUsername);
                actionMessage.addData("actionRequest", actionRequest);

                // Mesajı gönder
                networkManager.sendMessage(actionMessage);
                view.addSystemMessage("Hedef seçildi: " + selectedUsername);
                targetCombo.setValue(null);
            }
        });

        actionBox.getChildren().addAll(actionLabel, targetCombo, actionButton);
        view.getActionPanel().getChildren().add(actionBox);

        System.out.println("[DEBUG] ActionManager: Şerif araştırma aksiyonu eklendi");
    }

    private void addJailorExecuteAction() {
        System.out.println("[DEBUG] ActionManager: Gardiyan infaz aksiyonu ekleniyor...");

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("İnfaz Et:");

        Button actionButton = new Button("İnfaz Et");
        actionButton.getStyleClass().add("danger-button");
        actionButton.setOnAction(e -> {
            // Aksiyon mesajı oluştur
            Message actionMessage = new Message(MessageType.ACTION);
            ActionRequest actionRequest = new ActionRequest(ActionType.EXECUTE.name(), "prisoner");
            actionMessage.addData("actionRequest", actionRequest);

            // Mesajı gönder
            networkManager.sendMessage(actionMessage);
            view.addSystemMessage("İnfaz kararı verildi!");
        });

        actionBox.getChildren().addAll(actionLabel, actionButton);
        view.getActionPanel().getChildren().add(actionBox);

        System.out.println("[DEBUG] ActionManager: Gardiyan infaz aksiyonu eklendi");
    }

    private void addJailorJailAction() {
        System.out.println("[DEBUG] ActionManager: Gardiyan hapsetme aksiyonu ekleniyor...");

        // Kendisi dışındaki hayatta olan oyuncuları al
        String currentUsername = gameState.getCurrentUsername();
        List<Player> validTargets = gameState.getPlayers().stream()
                .filter(p -> p.isAlive() && !p.getUsername().equals(currentUsername))
                .collect(Collectors.toList());

        // Hedef listesi boşsa işlem yapma
        if (validTargets.isEmpty()) {
            view.addSystemMessage("Hapsedilecek oyuncu kalmadı.");
            return;
        }

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Hapse At:");

        // String tabanlı ComboBox oluştur
        ComboBox<String> targetCombo = new ComboBox<>();
        for (Player p : validTargets) {
            targetCombo.getItems().add(p.getUsername());
        }
        targetCombo.setPromptText("Hedef seçin");

        Button actionButton = new Button("Hapse At");
        actionButton.getStyleClass().add("warning-button");
        actionButton.setOnAction(e -> {
            String selectedUsername = targetCombo.getValue();
            if (selectedUsername != null) {
                // Aksiyon mesajı oluştur
                Message actionMessage = new Message(MessageType.ACTION);
                ActionRequest actionRequest = new ActionRequest(ActionType.JAIL.name(), selectedUsername);
                actionMessage.addData("actionRequest", actionRequest);

                // Mesajı gönder
                networkManager.sendMessage(actionMessage);
                view.addSystemMessage("Hapsedilecek hedef seçildi: " + selectedUsername);
                targetCombo.setValue(null);
            }
        });

        actionBox.getChildren().addAll(actionLabel, targetCombo, actionButton);
        view.getActionPanel().getChildren().add(actionBox);

        System.out.println("[DEBUG] ActionManager: Gardiyan hapsetme aksiyonu eklendi");
    }

    private void addVotingAction() {
        System.out.println("[DEBUG] ActionManager: Oylama aksiyonu ekleniyor...");

        // Kendisi dışındaki hayatta olan oyuncuları al
        String currentUsername = gameState.getCurrentUsername();
        List<Player> validTargets = gameState.getPlayers().stream()
                .filter(p -> p.isAlive() && !p.getUsername().equals(currentUsername))
                .collect(Collectors.toList());

        // Hedef listesi boşsa işlem yapma
        if (validTargets.isEmpty()) {
            view.addSystemMessage("Oylanacak oyuncu kalmadı.");
            return;
        }

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Oyla:");

        // String tabanlı ComboBox oluştur
        ComboBox<String> targetCombo = new ComboBox<>();
        for (Player p : validTargets) {
            targetCombo.getItems().add(p.getUsername());
        }
        targetCombo.setPromptText("Hedef seçin");

        Button actionButton = new Button("Oyla");
        actionButton.getStyleClass().add("secondary-button");
        actionButton.setOnAction(e -> {
            String selectedUsername = targetCombo.getValue();
            if (selectedUsername != null) {
                // Aksiyon mesajı oluştur
                Message voteMessage = new Message(MessageType.VOTE);
                VoteRequest voteRequest = new VoteRequest(selectedUsername);
                voteMessage.addData("voteRequest", voteRequest);

                // Mesajı gönder
                networkManager.sendMessage(voteMessage);
                view.addSystemMessage("Oy verildi: " + selectedUsername);
                targetCombo.setValue(null);
            }
        });

        actionBox.getChildren().addAll(actionLabel, targetCombo, actionButton);
        view.getActionPanel().getChildren().add(actionBox);

        System.out.println("[DEBUG] ActionManager: Oylama aksiyonu eklendi");
    }
}