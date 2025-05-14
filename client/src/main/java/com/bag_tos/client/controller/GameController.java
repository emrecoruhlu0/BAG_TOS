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
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.ArrayList;
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
        // Hapishane sohbet mesajı gönderme
        view.getJailChatPanel().setOnSendMessage(message -> {
            if (!message.isEmpty()) {
                // Sohbet mesajı için Message nesnesi oluştur
                Message chatMessage = new Message(MessageType.CHAT);
                ChatRequest chatRequest = new ChatRequest(message, "JAIL");
                chatMessage.addData("chatRequest", chatRequest);

                networkManager.sendMessage(chatMessage);
            }
        });

        // Oyuncu seçimi
        view.getPlayerListView().setOnPlayerSelected(player -> {
            System.out.println("Oyuncu seçildi: " + player.getUsername());
        });

        // Aksiyonları yapılandır
        setupActionHandlers();
    }

    public void handleJailEvent(String event, Message message) {
        switch (event) {
            case "JAIL_START":
                view.showJailChat();
                // Rolüne göre farklı mesaj göster
                Boolean isJailor = (Boolean) message.getDataValue("isJailor");
                if (isJailor != null && isJailor) {
                    view.addSystemMessage("Hapishane açıldı. Mahkum ile konuşabilirsiniz.");
                } else {
                    view.addSystemMessage("Hapishane hücresine hapsedildiniz.");
                }
                break;

            case "JAIL_END":
                view.hideJailChat();
                view.addSystemMessage("Hapishane hücresi kapatıldı.");
                break;

            case "PLAYER_JAILED":
                String jailedPlayer = (String) message.getDataValue("target");
                Boolean isPrisoner = (Boolean) message.getDataValue("isPrisoner");

                if (jailedPlayer != null && jailedPlayer.equals(gameState.getCurrentUsername())) {
                    view.showJailChat();
                    gameState.setData("isJailed", true);
                    view.addSystemMessage("Bu gece hapsedildiniz! Gardiyan ile konuşabilirsiniz.");
                }
                break;

            case "JAILOR_ACTIVE":
                String prisoner = (String) message.getDataValue("prisoner");
                if (prisoner != null) {
                    view.showJailChat();
                    view.addSystemMessage(prisoner + " adlı oyuncuyu hapsettiniz.");
                }
                break;
        }
    }

    public void updatePhaseDisplay() {
        GameState.Phase currentPhase = gameState.getCurrentPhase();
        System.out.println("updatePhaseDisplay çağrıldı, mevcut faz: " + currentPhase);

        // UI thread kontrolü ekle
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updatePhaseDisplay());
            return;
        }

        try {
            view.updatePhase(currentPhase);

            // Faz değişince chat kontrollerini güncelle
            updateChatControls();

            // Faz değişince aksiyon panelini güncelle (gecikmeli)
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                try {
                                    System.out.println("Faz değişiminden sonra aksiyonlar yeniden yükleniyor...");
                                    updateActionsOnly();
                                } catch (Exception e) {
                                    System.err.println("Aksiyon güncellemesi sırasında hata: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        }
                    }, 300  // 300ms gecikme
            );

            System.out.println("Faz gösterimi güncellendi: " + currentPhase);
        } catch (Exception e) {
            System.err.println("Faz gösterimi güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void setupActionHandlers() {
        view.getActionPanel().clearActions();

        System.out.println("setupActionHandlers çağrıldı");
        System.out.println("Faz: " + gameState.getCurrentPhase() + ", Rol: " + gameState.getCurrentRole());

        // Oyuncu listesi kontrolü
        if (gameState.getPlayers().isEmpty()) {
            System.out.println("UYARI: Oyuncu listesi boş, aksiyonlar eklenemiyor!");
            return;
        }

        // Geçerli oyuncu bilgileri
        String currentUsername = gameState.getCurrentUsername();
        String currentRole = gameState.getCurrentRole();

        // Hapse alınan kişi kontrolü - YENİ KOD
        Boolean isJailed = (Boolean) gameState.getData("isJailed");
        if (isJailed != null && isJailed) {
            System.out.println("Oyuncu hapsedilmiş, aksiyon paneli boş bırakılıyor");
            view.addSystemMessage("Hapsedildiniz, herhangi bir aksiyon gerçekleştiremezsiniz.");
            return;
        }

        // Jester kontrolü - gece aksiyonu yok
        if (currentRole.equals("Jester") && gameState.getCurrentPhase() == GameState.Phase.NIGHT) {
            System.out.println("Jester rolü, gece aksiyonu yok");
            view.addSystemMessage("Jester olarak gece aksiyonunuz bulunmuyor.");
            return;
        }

        // Rol ve faza uygun oyuncuları aksiyon paneline ekle
        if (currentRole != null && currentRole.equals("Doktor")) {
            view.getActionPanel().setAlivePlayers(gameState.getPlayers());
        } else {
            view.getActionPanel().setAlivePlayers(gameState.getPlayers(), currentUsername);
        }

        System.out.println("Hedef sayısı: " + view.getActionPanel().getTargetCount());

        // Rol ve faza göre aksiyonları yapılandır
        GameState.Phase phase = gameState.getCurrentPhase();

        if (phase == GameState.Phase.NIGHT) {
            if (currentRole.equals("Mafya")) {
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
            } else if (currentRole.equals("Doktor")) {
                // Doktor iyileştirme aksiyonu
                view.getActionPanel().addHealAction(target -> {
                    // Kendi üzerinde aksiyon kontrolü - Doktor rolüne özel muamele
                    if (!GameConfig.ALLOW_SELF_ACTIONS &&
                            target.getUsername().equals(gameState.getCurrentUsername()) &&
                            !"Doktor".equals(currentRole)) {
                        view.addSystemMessage("Kendiniz üzerinde aksiyon yapamazsınız!");
                        return;
                    }

                    // İyileştirme aksiyonu için Message nesnesi oluştur
                    Message actionMessage = new Message(MessageType.ACTION);
                    ActionRequest actionRequest = new ActionRequest(ActionType.HEAL.name(), target.getUsername());
                    actionMessage.addData("actionRequest", actionRequest);

                    networkManager.sendMessage(actionMessage);
                    view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                });
            } else if (currentRole.equals("Serif")) {
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
            } else if (currentRole.equals("Gardiyan")) {
                // Gardiyanın infaz etme aksiyonu (gece)
                view.getActionPanel().addExecuteAction(target -> {
                    Message actionMessage = new Message(MessageType.ACTION);
                    ActionRequest actionRequest = new ActionRequest(ActionType.EXECUTE.name(), "prisoner");
                    actionMessage.addData("actionRequest", actionRequest);

                    networkManager.sendMessage(actionMessage);
                    view.addSystemMessage("İnfaz kararı verildi!");
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

            // Gardiyan hapsetme aksiyonu (gündüz)
            if (currentRole.equals("Gardiyan")) {
                view.getActionPanel().addJailAction(target -> {
                    if (!GameConfig.ALLOW_SELF_ACTIONS &&
                            target.getUsername().equals(gameState.getCurrentUsername())) {
                        view.addSystemMessage("Kendinizi hapse atamazsınız!");
                        return;
                    }

                    Message actionMessage = new Message(MessageType.ACTION);
                    ActionRequest actionRequest = new ActionRequest(ActionType.JAIL.name(), target.getUsername());
                    actionMessage.addData("actionRequest", actionRequest);

                    networkManager.sendMessage(actionMessage);
                    view.addSystemMessage("Hapsedilecek oyuncu seçildi: " + target.getUsername());
                });
            }
        }

        System.out.println("Aksiyon handler'ları başarıyla kuruldu, faz: " + phase + ", rol: " + currentRole);
    }
    // MessageHandler tarafından kullanılacak metot (sunucudan gelen aksiyonlara göre UI'ı günceller)
    public void updateActions(List<String> availableActions) {
        Platform.runLater(() -> {
            try {
                view.getActionPanel().clearActions();

                view.getActionPanel().setVisible(true);

                // Geçerli oyuncu bilgileri
                String currentUsername = gameState.getCurrentUsername();
                String currentRole = gameState.getCurrentRole();

                System.out.println("Aksiyon güncelleniyor - Rol: " + currentRole + ", Kullanıcı: " + currentUsername);

                // Hapse alınan kişi kontrolü
                Boolean isJailed = (Boolean) gameState.getData("isJailed");
                if (isJailed != null && isJailed && gameState.getCurrentPhase() == GameState.Phase.NIGHT) {
                    System.out.println("Oyuncu hapsedilmiş, aksiyon paneli boş bırakılıyor");
                    view.addSystemMessage("Hapsedildiniz, herhangi bir aksiyon gerçekleştiremezsiniz.");
                    return;
                }

                // Jester kontrolü - gece aksiyonu yok
                if (currentRole.equals("Jester") && gameState.getCurrentPhase() == GameState.Phase.NIGHT) {
                    System.out.println("Jester rolü, gece aksiyonu yok");
                    view.addSystemMessage("Jester olarak gece aksiyonunuz bulunmuyor.");
                    return;
                }

                // İnaktif Jailor kontrolü
                if (availableActions != null && availableActions.contains("NO_ACTION") &&
                        currentRole.equals("Gardiyan") && gameState.getCurrentPhase() == GameState.Phase.NIGHT) {
                    showInactiveJailorMessage();
                    return;
                }

                // Oyuncu listesi kontrolü
                if (gameState.getPlayers().isEmpty()) {
                    System.out.println("UYARI: updateActions'da oyuncu listesi boş, aksiyonlar güncellenemiyor!");
                    return;
                }

                // Mafya için özel hedef filtreleme
                if ("Mafya".equals(currentRole) && gameState.getCurrentPhase() == GameState.Phase.NIGHT) {
                    System.out.println("Mafya filtresi uygulanıyor...");

                    // Mafya üyelerini topla
                    List<String> mafiaUsernames = new ArrayList<>();
                    for (Player p : gameState.getPlayers()) {
                        if ("Mafya".equals(p.getRole())) {
                            mafiaUsernames.add(p.getUsername());
                            System.out.println("Mafya üyesi tespit edildi: " + p.getUsername());
                        }
                    }

                    // Filtrelenmiş oyuncu listesi oluştur
                    List<Player> filteredPlayers = new ArrayList<>();
                    for (Player p : gameState.getPlayers()) {
                        if (p.isAlive() && !p.getUsername().equals(currentUsername) &&
                                !mafiaUsernames.contains(p.getUsername())) {
                            filteredPlayers.add(p);
                            System.out.println("Mafya hedef listesine eklendi: " + p.getUsername());
                        }
                    }

                    // Filtrelenmiş listeyi ayarla
                    view.getActionPanel().setCustomTargetList(filteredPlayers);

                } else if ("Doktor".equals(currentRole)) {
                    // Doktor kendi üzerine aksiyon yapabilir
                    view.getActionPanel().setAlivePlayers(gameState.getPlayers());

                } else {
                    // Diğer roller için kendisi hariç
                    view.getActionPanel().setAlivePlayers(gameState.getPlayers(), currentUsername);
                }

                int targetCount = view.getActionPanel().getTargetCount();
                System.out.println("Toplam hedef sayısı: " + targetCount);

                if (targetCount == 0) {
                    System.out.println("UYARI: Hedef listesi boş, aksiyonlar eklenmiyor!");
                    return;
                }

                if (availableActions != null && !availableActions.isEmpty()) {
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
                                    // Doktor rolü için özel durum kontrolü
                                    if (!GameConfig.ALLOW_SELF_ACTIONS &&
                                            target.getUsername().equals(gameState.getCurrentUsername()) &&
                                            !"Doktor".equals(gameState.getCurrentRole())) {
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
                                    try {
                                        // İnfaz aksiyonu için mesaj oluştur
                                        Message actionMessage = new Message(MessageType.ACTION);
                                        ActionRequest actionRequest = new ActionRequest(ActionType.EXECUTE.name(), "prisoner");
                                        actionMessage.addData("actionRequest", actionRequest);

                                        boolean sent = networkManager.sendMessage(actionMessage);
                                        if (sent) {
                                            view.addSystemMessage("İnfaz kararı verildi!");
                                        } else {
                                            view.addSystemMessage("HATA: İnfaz aksiyonu gönderilemedi!");
                                        }

                                        // Debug için network durumunu kontrol et
                                        System.out.println("İnfaz aksiyonu gönderildi, bağlantı durumu: " +
                                                (networkManager.isConnected() ? "Bağlı" : "Bağlı değil"));
                                    } catch (Exception e) {
                                        System.err.println("İnfaz aksiyonu gönderilirken hata: " + e.getMessage());
                                        e.printStackTrace();
                                        view.addSystemMessage("İnfaz aksiyonu gönderilirken hata oluştu!");
                                    }
                                });
                                break;
                        }
                    }

                    System.out.println(availableActions.size() + " aksiyon başarıyla eklendi");
                } else {
                    System.out.println("Kullanılabilir aksiyon yok, panel temizlendi");
                }
            } catch (Exception e) {
                System.err.println("Aksiyonlar güncellenirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void showInactiveJailorMessage() {
        Platform.runLater(() -> {
            try {
                view.getActionPanel().clearActions();
                view.addSystemMessage("Gündüz fazında kimseyi hapsetmediniz. Bu gece aksiyon gerçekleştiremeyeceksiniz.");

                // Panel tamamen kapatılmıyor, sadece "Aksiyon yok" mesajı gösteriliyor
                Label noActionLabel = new Label("Bu gece aksiyon yok");
                noActionLabel.getStyleClass().add("warning-text");
                view.getActionPanel().getChildren().add(noActionLabel);

                System.out.println("İnaktif Jailor mesajı gösterildi");
            } catch (Exception e) {
                System.err.println("İnaktif Jailor mesajı gösterilirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void forceUpdateActionPanel() {
        Platform.runLater(() -> {
            if (gameState.getPlayers().isEmpty()) {
                System.out.println("UYARI: Oyuncu listesi forceUpdateActionPanel'de boş!");

                // Oyuncu listesi boşsa, tüm oyuncuları almanın başka bir yolunu deneyebiliriz
                // Örneğin sunucudan yeni bir oyuncu listesi isteği gönderebiliriz
                // veya varsayılan hedefler belirleyebiliriz

                // Şimdilik geçici bir çözüm olarak tüm aksiyon butonlarını devre dışı bırakalım
                view.getActionPanel().clearActions();
                view.getActionPanel().setDisable(true);

                // Kullanıcıya bilgi ver
                view.addSystemMessage("Hedef listesi henüz hazır değil, lütfen bekleyin...");

                // Belirli bir süre sonra tekrar dene
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> forceUpdateActionPanel());
                            }
                        },
                        2000  // 2 saniye sonra tekrar dene
                );

                return;
            }

            // Oyuncu listesi doluysa normal güncelleme yap
            view.getActionPanel().setDisable(false);
            updateActions(List.of(gameState.getAvailableAction().split(", ")));
            System.out.println("Aksiyon paneli zorla güncellendi, oyuncu sayısı: " + gameState.getPlayers().size());
        });
    }

    public void updateUI() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateUI());
            return;
        }

        try {
            System.out.println("Tam UI güncellemesi başlatılıyor...");

            updatePhaseDisplay();
            updateRoleDisplay();
            updatePlayerListDisplay();
            updateTimeDisplay();

            // Gecikmeli aksiyonları yükleme
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                try {
                                    updateActionControls();
                                    updateChatControls();

                                    // Oyuncu adını güncelle
                                    view.updateUsername(gameState.getCurrentUsername());

                                    System.out.println("Tam UI güncellemesi tamamlandı");
                                } catch (Exception e) {
                                    System.err.println("Gecikmeli UI güncellemesi sırasında hata: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        }
                    }, 100  // 100ms gecikme
            );
        } catch (Exception e) {
            System.err.println("UI güncellemesi sırasında hata: " + e.getMessage());
            e.printStackTrace();
        }
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
        // UI thread kontrolü
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateChatControls());
            return;
        }

        try {
            // Gece/gündüz durumuna göre sohbet kontrollerini güncelle
            GameState.Phase currentPhase = gameState.getCurrentPhase();
            boolean isNight = currentPhase == GameState.Phase.NIGHT;
            String currentRole = gameState.getCurrentRole();

            // Debug log
            System.out.println("Chat kontrollerini güncelleme - Faz: " + currentPhase + ", Rol: " + currentRole);

            // Gece fazında genel sohbeti devre dışı bırak
            if (isNight && GameConfig.DISABLE_CHAT_AT_NIGHT) {
                view.getChatPanel().getMessageField().setDisable(true);
                view.getChatPanel().getSendButton().setDisable(true);
                System.out.println("Gece fazı - genel sohbet devre dışı");
            } else {
                view.getChatPanel().getMessageField().setDisable(false);
                view.getChatPanel().getSendButton().setDisable(false);
                System.out.println("Gündüz fazı veya sohbet yasağı yok - genel sohbet etkin");
            }

            // Mafya sohbeti güncelleme
            boolean isMafia = "Mafya".equals(currentRole);
            view.getMafiaChatPanel().getMessageField().setDisable(!isMafia);
            view.getMafiaChatPanel().getSendButton().setDisable(!isMafia);
            System.out.println("Mafya sohbeti " + (isMafia ? "etkin" : "devre dışı"));

            // Ölü oyuncu kontrolü
            if (!gameState.isAlive() && !GameConfig.ALLOW_DEAD_CHAT) {
                view.getChatPanel().getMessageField().setDisable(true);
                view.getChatPanel().getSendButton().setDisable(true);
                view.getMafiaChatPanel().getMessageField().setDisable(true);
                view.getMafiaChatPanel().getSendButton().setDisable(true);
                System.out.println("Ölü oyuncu - tüm sohbetler devre dışı");
            }
        } catch (Exception e) {
            System.err.println("Sohbet kontrolleri güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
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
        // UI thread kontrolü ekle
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateActionsOnly());
            return;
        }

        try {
            // Debug bilgi ekle
            System.out.println("UpdateActionsOnly çağrıldı, faz: " + gameState.getCurrentPhase());

            // Aksiyon panelini temizle
            view.getActionPanel().clearActions();

            // Oyuncu listesi kontrolü
            if (gameState.getPlayers().isEmpty()) {
                System.out.println("UYARI: Oyuncu listesi boş, aksiyonlar güncellenemiyor!");
                return;
            }

            // Mevcut role ve faza göre aksiyonları yeniden yükle
            String availableAction = gameState.getAvailableAction();
            System.out.println("Kullanılabilir aksiyonlar: " + availableAction);

            if (availableAction != null && !availableAction.isEmpty()) {
                String[] actions = availableAction.split(", ");
                updateActions(List.of(actions));
                System.out.println("Aksiyon paneli güncellendi, " + actions.length + " aksiyon yüklendi");
            } else {
                setupActionHandlers();
                System.out.println("Aksiyon paneli güncellendi, temel handler'lar kuruldu");
            }
        } catch (Exception e) {
            System.err.println("Aksiyon güncellemesi sırasında hata: " + e.getMessage());
            e.printStackTrace();
        }
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

    public void clearActionPanel() {
        Platform.runLater(() -> {
            try {
                view.getActionPanel().clearActions();
                view.getActionPanel().setVisible(false);
                System.out.println("Aksiyon paneli gizlendi - Pasif Jailor için");
            } catch (Exception e) {
                System.err.println("Aksiyon paneli gizlenirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}