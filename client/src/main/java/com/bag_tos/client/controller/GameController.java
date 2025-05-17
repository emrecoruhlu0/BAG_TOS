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
import com.bag_tos.common.audio.AudioFormat;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// İmportlar
import com.bag_tos.client.audio.VoiceChatManager;

public class GameController {
    private GameView view;
    private GameState gameState;
    private NetworkManager networkManager;
    private Stage primaryStage;
    private ActionManager actionManager;
    private Map<String, String> selectedAvatars = new HashMap<>();
    private VoiceChatManager voiceChatManager;

    // Ses bağlantısı kontrolü için zamanlayıcı
    private java.util.Timer voiceConnectionTimer;

    private java.util.Timer uiUpdateTimer;

    private GameState.Phase lastPhase = null;
    private String lastRole = "";
    private int lastTime = -1;
    private int lastPlayerCount = 0;

    private boolean firstUpdate = true;


    public GameController(Stage primaryStage, GameState gameState, NetworkManager networkManager) {
        this.primaryStage = primaryStage;
        this.gameState = gameState;
        this.networkManager = networkManager;
        this.view = new GameView();

        // Başlangıçta görünümü mevcut faza göre ayarla
        GameState.Phase currentPhase = gameState.getCurrentPhase();
        view.updatePhase(currentPhase);
        System.out.println("GameController başlangıç fazı: " + currentPhase);

        // ActionManager'ı oluştur
        this.actionManager = new ActionManager(gameState, networkManager, view);

        // Ses yöneticisini oluştur
        this.voiceChatManager = new VoiceChatManager();

        this.voiceConnectionTimer = null;
        this.uiUpdateTimer = null;
        this.lastPhase = gameState.getCurrentPhase();
        this.lastRole = gameState.getCurrentRole();
        this.lastTime = gameState.getRemainingTime();
        this.lastPlayerCount = gameState.getPlayers().size();

        // Ses kontrol paneli olayını bağla
        view.getVoiceControlPanel().setMicrophoneStateChangeListener(active -> {
            voiceChatManager.setMicrophoneActive(active);
        });

        configureView();
        updateUI();

        initializeVoiceSystem();  // <--- BU SATIRI EKLEYİN

        initializePlayerCircle();

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

        if (view.getPlayerListView() != null) {
            view.getPlayerListView().setOnPlayerSelected(player -> {
                System.out.println("Oyuncu seçildi: " + player.getUsername());
            });
        }

        // Aksiyonları yapılandır
        setupActionHandlers();

        // Oyun durumu değişikliklerini dinle
        gameState.currentPhaseProperty().addListener((obs, oldPhase, newPhase) -> {
            // Faz değişikliğini ses sistemine bildir
            boolean isNight = (newPhase == GameState.Phase.NIGHT);
            voiceChatManager.setNightPhase(isNight);

            // Gece fazında mikrofon butonunu devre dışı bırak
            Platform.runLater(() -> {
                view.getVoiceControlPanel().setMicrophoneEnabled(!isNight);
                if (isNight) {
                    view.getVoiceControlPanel().setMicrophoneActive(false);
                }
            });
        });

        // Canlılık durumu değişikliklerini dinle
        gameState.aliveProperty().addListener((obs, wasAlive, isAlive) -> {
            // Hayatta olma durumunu ses sistemine bildir
            voiceChatManager.setPlayerAlive(isAlive);

            // Öldüğünde mikrofon butonunu devre dışı bırak
            if (!isAlive) {
                Platform.runLater(() -> {
                    view.getVoiceControlPanel().setMicrophoneEnabled(false);
                    view.getVoiceControlPanel().setMicrophoneActive(false);
                });
            }
        });
    }

    /**
     * Ses sistemini başlatır
     */
    private void initializeVoiceSystem() {
        try {
            // Ses sistemini başlat (eğer henüz başlatılmamışsa)
            if (voiceChatManager == null) {
                voiceChatManager = new VoiceChatManager();
            }

            // Simülasyon modunu etkinleştir
            voiceChatManager.setSimulationMode(false);

            // Sunucu bilgileri
            String serverAddress = networkManager.getServerAddress(); // Bu metod yoksa değiştirin
            int voicePort = AudioFormat.DEFAULT_VOICE_PORT;
            String username = gameState.getCurrentUsername();

            // Eğer getServerAddress() metodu yoksa, NetworkManager'dan bilgileri al
            if (serverAddress == null) {
                serverAddress = "localhost"; // Varsayılan değer
            }

            // Simülasyon modu ile başlat
            boolean initialized = voiceChatManager.initializeWithSimulation(serverAddress, voicePort, username);

            if (initialized) {
                System.out.println("[SES] Ses sistemi simülasyon modu ile başarıyla başlatıldı");

                // Ses kontrollerini etkinleştir
                view.getVoiceControlPanel().setMicrophoneEnabled(true);

                // Faz durumuna göre ayarla
                boolean isNight = gameState.getCurrentPhase() == GameState.Phase.NIGHT;
                voiceChatManager.setNightPhase(isNight);

                // Ses bağlantısı kontrolü için timer başlat
                startVoiceConnectionChecker();
            } else {
                System.err.println("[SES] Ses sistemi başlatılamadı!");

                // Ses kontrollerini devre dışı bırak
                view.getVoiceControlPanel().setMicrophoneEnabled(false);
            }
        } catch (Exception e) {
            System.err.println("[SES] Ses sistemi başlatılırken hata: " + e.getMessage());
            e.printStackTrace();

            // Ses kontrollerini devre dışı bırak
            view.getVoiceControlPanel().setMicrophoneEnabled(false);
        }
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

        // Hapse alınan kişi kontrolü
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
        if (currentRole.equals("Doktor")) {
            view.getActionPanel().setAlivePlayers(gameState.getPlayers());
        } else if (currentRole.equals("Mafya")) {
            // Tüm mafya üyelerini bul
            List<String> mafiaUsernames = gameState.getPlayers().stream()
                    .filter(p -> "Mafya".equals(p.getRole()))
                    .map(Player::getUsername)
                    .collect(Collectors.toList());

            // Sadece mafya olmayan ve hayatta olan oyuncuları filtrele
            List<Player> nonMafiaTargets = gameState.getPlayers().stream()
                    .filter(Player::isAlive)
                    .filter(p -> !mafiaUsernames.contains(p.getUsername()))
                    .collect(Collectors.toList());

            // Özel bir hedef listesi ayarla
            view.getActionPanel().setManualTargetList(nonMafiaTargets);
        } else {
            view.getActionPanel().setAlivePlayers(gameState.getPlayers(), currentUsername);
        }

        System.out.println("Hedef sayısı: " + view.getActionPanel().getTargetCount());

        // Rol ve faza göre aksiyonları yapılandır
        GameState.Phase phase = gameState.getCurrentPhase();

        if (phase == GameState.Phase.NIGHT) {
            switch (currentRole) {
                case "Mafya":
                    // Mafya üyelerini belirle
                    List<String> mafiaUsernames = gameState.getPlayers().stream()
                            .filter(p -> "Mafya".equals(p.getRole()))
                            .map(Player::getUsername)
                            .collect(Collectors.toList());

                    // Mafya oyuncuları kendilerini de gördüğünden, kendinizi de listeye ekleyin
                    if (!mafiaUsernames.contains(currentUsername)) {
                        mafiaUsernames.add(currentUsername);
                    }

                    // Debug için mafya listesini yazdır
                    System.out.println("Mafya üyeleri: " + String.join(", ", mafiaUsernames));

                    // Mafya öldürme aksiyonu - mafya üyelerini hariç tut
                    view.getActionPanel().addKillAction(target -> {
                        // Öldürme aksiyonu için Message nesnesi oluştur
                        Message actionMessage = new Message(MessageType.ACTION);
                        ActionRequest actionRequest = new ActionRequest(ActionType.KILL.name(), target.getUsername());
                        actionMessage.addData("actionRequest", actionRequest);

                        networkManager.sendMessage(actionMessage);
                        view.addSystemMessage("Hedef seçildi: " + target.getUsername());
                    }, mafiaUsernames);
                    break;
                case "Doktor":
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
                    break;
                case "Serif":
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
                    break;
                case "Gardiyan":
                    // Gardiyanın infaz etme aksiyonu (gece)
                    view.getActionPanel().addExecuteAction(target -> {
                        Message actionMessage = new Message(MessageType.ACTION);
                        ActionRequest actionRequest = new ActionRequest(ActionType.EXECUTE.name(), "prisoner");
                        actionMessage.addData("actionRequest", actionRequest);

                        networkManager.sendMessage(actionMessage);
                        view.addSystemMessage("İnfaz kararı verildi!");
                    });
                    break;
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
        // ActionManager'ı kullan
        actionManager.updateActions();
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

    public void updateUI() {
        if (Platform.isFxApplicationThread()) {
            updateUIInternal();
        } else {
            Platform.runLater(this::updateUIInternal);
        }
    }

    private void updateUIInternal() {
        try {

            if (firstUpdate) {
                // Oyun ekranına ilk geçişte fazı güncelle
                firstUpdate = false;

                // Mevcut fazı zorla güncelle
                GameState.Phase currentPhase = gameState.getCurrentPhase();
                System.out.println("İlk UI güncellemesi: Faz = " + currentPhase);

                // Fazı view'da güncelle
                view.updatePhase(currentPhase);

                // Eğer lobi fazında değilsek, fazı zorunlu olarak uygula
                if (currentPhase != GameState.Phase.LOBBY) {
                    System.out.println("Fazı zorunlu olarak güncelleme: " + currentPhase);
                    updatePhaseDisplay();

                    // Faz durumuna göre chat ve aksiyon kontrollerini ayarla
                    updateChatControls();
                    setupActionHandlers();
                }
            }

            System.out.println("Tam UI güncellemesi başlatılıyor...");

            // Önceki Timer'ı iptal et
            if (uiUpdateTimer != null) {
                uiUpdateTimer.cancel();
                uiUpdateTimer = null;
            }

            // Sadece değişen şeyleri güncelle, tümünü değil
            boolean phaseChanged = false;
            boolean roleChanged = false;
            boolean playersChanged = false;
            boolean timeChanged = false;

            // Faz değiştiyse sadece faz gösterimini güncelle
            if (lastPhase != gameState.getCurrentPhase()) {
                lastPhase = gameState.getCurrentPhase();
                updatePhaseDisplay();
                phaseChanged = true;
            }

            // Rol değiştiyse sadece rol gösterimini güncelle
            if (lastRole == null || !lastRole.equals(gameState.getCurrentRole())) {
                lastRole = gameState.getCurrentRole();
                updateRoleDisplay();
                roleChanged = true;
            }

            // Zaman değiştiyse sadece zaman gösterimini güncelle
            if (lastTime != gameState.getRemainingTime()) {
                lastTime = gameState.getRemainingTime();
                updateTimeDisplay();
                timeChanged = true;
            }

            // Oyuncu listesi değiştiyse sadece oyuncu listesini güncelle
            if (havePlayersChanged()) {
                updatePlayerListInternal();
                playersChanged = true;
            }

            // Eğer herhangi bir değişiklik olduysa, aksiyon ve sohbet kontrollerini güncelle
            if (phaseChanged || roleChanged || playersChanged) {
                // Yeni bir timer oluştur
                uiUpdateTimer = new java.util.Timer();
                uiUpdateTimer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            try {
                                updateActionControls();
                                updateChatControls();
                                System.out.println("Gecikmeli UI güncellemesi tamamlandı");
                            } catch (Exception e) {
                                System.err.println("Gecikmeli UI güncellemesi sırasında hata: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    }
                }, 100);
            }

            // Ses sistemini oyun durumuyla senkronize et
            voiceChatManager.synchronizeWithGameState(gameState);

            System.out.println("Tam UI güncellemesi tamamlandı - Değişiklikler: " +
                    "Faz=" + phaseChanged + ", Rol=" + roleChanged +
                    ", Oyuncular=" + playersChanged + ", Zaman=" + timeChanged);

        } catch (Exception e) {
            System.err.println("UI güncellemesi sırasında hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean havePlayersChanged() {
        // Son oyuncu listesi kontrolü
        if (lastPlayerCount != gameState.getPlayers().size()) {
            lastPlayerCount = gameState.getPlayers().size();
            return true;
        }

        // Oyuncuların değişip değişmediğini daha detaylı kontrol etmek için
        // hash veya version numarası yaklaşımı kullanılabilir

        return false; // Basitleştirmek için
    }

    // Periyodik ses bağlantısı kontrolü
    private void startVoiceConnectionChecker() {
        // Önceki timer'ı temizle
        if (voiceConnectionTimer != null) {
            voiceConnectionTimer.cancel();
        }

        // Yeni timer başlat
        voiceConnectionTimer = new java.util.Timer("VoiceConnectionChecker", true);
        voiceConnectionTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // Ses bağlantısını kontrol et
                if (voiceChatManager != null && voiceChatManager.isInitialized()) {
                    voiceChatManager.checkVoiceConnection();
                }
            }
        }, 5000, 30000); // İlk 5 saniye sonra, sonra her 30 saniyede bir
    }

    private void updateRoleDisplay() {
        view.updateRole(gameState.getCurrentRole());
    }

    private void updatePlayerListDisplay() {
        // Güncel oyuncu listesini al
        List<Player> currentPlayers = gameState.getPlayers();

        // ÖNEMLİ: Her iki listeyi de güncelle
        // Eski PlayerListView güncellemesi
        view.getPlayerListView().updatePlayers(currentPlayers);

        // YENİ: PlayerCircleView güncellemesi ekleyin
        if (view.getPlayerCircleView() != null) {
            view.getPlayerCircleView().updatePlayers(currentPlayers);
        }

        // Debug: Oyuncu isimlerini ve avatarları logla
        System.out.println("Oyuncu Listesi Güncellendi, Oyuncu Sayısı: " + currentPlayers.size());
        for (Player player : currentPlayers) {
            System.out.println("Oyuncu: " + player.getUsername() +
                    ", Avatar: " + player.getAvatarId() +
                    ", Hayatta: " + player.isAlive());
        }
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

    public void updateTimeOnly() {
        // Avoid scheduling UI updates if we're already on the UI thread
        if (Platform.isFxApplicationThread()) {
            // Update only the time display component
            view.updateTime(gameState.getRemainingTime());
        } else {
            // Schedule a minimal UI update on the JavaFX thread
            Platform.runLater(() -> {
                // Update only the time display component
                view.updateTime(gameState.getRemainingTime());
            });
        }
    }

    // GameController sınıfında, updatePlayerListOnly metodunu değiştir
    public void updatePlayerListOnly() {
        // Eğer zaten UI thread'inde isek, direkt çalıştır
        if (Platform.isFxApplicationThread()) {
            updatePlayerListInternal();
        } else {
            // Değilse Platform.runLater kullan
            Platform.runLater(this::updatePlayerListInternal);
        }
    }

    public void updateActionsOnly() {
        Platform.runLater(() -> {
            // Özel mafya kontrolü
            if ("Mafya".equals(gameState.getCurrentRole()) &&
                    gameState.getCurrentPhase() == GameState.Phase.NIGHT) {

                // Doğrudan fix uygula
                directMafiaFix();
                return; // Diğer kodları çalıştırma
            }
            actionManager.updateActions();
        });
    }

    private void updatePlayerListInternal() {
        try {
            // Güncel oyuncu listesini al - güvenli kopya oluştur
            List<Player> currentPlayers = new ArrayList<>(gameState.getPlayers());

            // Eğer liste boşsa erken çık
            if (currentPlayers.isEmpty()) {
                System.out.println("UYARI: Oyuncu listesi boş, güncelleme atlanıyor.");
                return;
            }

            // Oyuncuların avatarlarını ayarla
            for (Player player : currentPlayers) {
                // Eğer avatarId null veya boş ise, seçilen avatarı kullan
                if (player.getAvatarId() == null || player.getAvatarId().isEmpty()) {
                    String savedAvatarId = selectedAvatars.get(player.getUsername());
                    if (savedAvatarId != null) {
                        player.setAvatarId(savedAvatarId);
                    } else {
                        // Varsayılan avatar
                        player.setAvatarId("avatar1");
                    }
                }
            }

            // Oyuncu listesini logla
            System.out.println("OYUNCU LİSTESİ GÜNCELLENİYOR - Toplam: " + currentPlayers.size());
            for (Player p : currentPlayers) {
                System.out.println("  - Oyuncu: " + p.getUsername() +
                        ", Avatar: " + p.getAvatarId() +
                        ", Rol: " + p.getRole() +
                        ", Hayatta: " + p.isAlive());
            }

            // PlayerCircleView güncellemesi
            if (view.getPlayerCircleView() != null) {
                System.out.println("PlayerCircleView güncelleniyor...");
                view.getPlayerCircleView().updatePlayers(currentPlayers);
            } else {
                System.out.println("HATA: PlayerCircleView null!");
            }

            // Eski liste görünümü hala kullanılıyorsa onu da güncelle
            if (view.getPlayerListView() != null && view.getPlayerListView().isVisible()) {
                view.getPlayerListView().updatePlayers(currentPlayers);
            }

        } catch (Exception e) {
            System.err.println("Oyuncu listesi güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initializePlayerCircle() {
        // İlk kez oyuncu listesini güncelle ve bir süre sonra tekrar dene
        updatePlayerListOnly();

        // Ekran boyutları tam olarak hesaplanmış olsun diye
        // kısa bir gecikme sonra tekrar güncelle
        Platform.runLater(() -> {
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> updatePlayerListOnly());
                        }
                    },
                    500 // 500ms sonra tekrar dene
            );
        });
    }

    private void directMafiaFix() {
        // Bu metot doğrudan GameController içinde çağrılmalıdır

        // Aksiyon panelini temizle
        view.getActionPanel().clearActions();

        // Mafya üyelerini topla
        List<String> mafiaUsernames = new ArrayList<>();
        for (Player p : gameState.getPlayers()) {
            if ("Mafya".equals(p.getRole())) {
                mafiaUsernames.add(p.getUsername());
                System.out.println("DEBUG: Mafya üyesi: " + p.getUsername());
            }
        }

        // Mafya olmayan canlı oyuncular
        List<Player> validTargets = new ArrayList<>();
        for (Player p : gameState.getPlayers()) {
            if (p.isAlive() && !mafiaUsernames.contains(p.getUsername())) {
                validTargets.add(p);
                System.out.println("DEBUG: Geçerli hedef: " + p.getUsername());
            }
        }

        // Aksiyon kutusu oluştur
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Öldür:");

        // String tabanlı ComboBox
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
                // Mesaj gönder
                Message actionMessage = new Message(MessageType.ACTION);
                ActionRequest actionRequest = new ActionRequest(ActionType.KILL.name(), selectedUsername);
                actionMessage.addData("actionRequest", actionRequest);

                networkManager.sendMessage(actionMessage);
                view.addSystemMessage("Hedef seçildi: " + selectedUsername);
                targetCombo.setValue(null);
            }
        });

        actionBox.getChildren().addAll(actionLabel, targetCombo, actionButton);
        view.getActionPanel().getChildren().add(actionBox);
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
        // Ses bağlantısı timer'ını durdur
        if (voiceConnectionTimer != null) {
            voiceConnectionTimer.cancel();
            voiceConnectionTimer = null;
        }

        // Ses sistemini kapat
        if (voiceChatManager != null) {
            voiceChatManager.shutdown();
        }

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
        // Ses bağlantısı timer'ını durdur
        if (voiceConnectionTimer != null) {
            voiceConnectionTimer.cancel();
            voiceConnectionTimer = null;
        }

        // Ses sistemini kapat
        if (voiceChatManager != null) {
            voiceChatManager.shutdown();
        }

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

    public void setSelectedAvatar(String username, String avatarId) {
        selectedAvatars.put(username, avatarId);
        System.out.println("Seçilen avatar kaydedildi: " + username + " -> " + avatarId);
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