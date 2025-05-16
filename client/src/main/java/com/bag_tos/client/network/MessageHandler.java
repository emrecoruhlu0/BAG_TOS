package com.bag_tos.client.network;

import com.bag_tos.client.controller.GameController;
import com.bag_tos.client.controller.LobbyController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.response.*;
import com.bag_tos.common.model.ActionType;
import com.bag_tos.common.model.PlayerInfo;
import com.bag_tos.common.util.JsonUtils;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageHandler implements NetworkManager.MessageListener {
    private GameController gameController;
    private LobbyController lobbyController;
    private GameState gameState;

    private boolean initialPlayerUpdateDone = false;

    // GameController için constructor
    public MessageHandler(GameController gameController, GameState gameState) {
        this.gameController = gameController;
        this.lobbyController = null;
        this.gameState = gameState;
    }

    // LobbyController için constructor
    public MessageHandler(LobbyController lobbyController, GameState gameState) {
        this.lobbyController = lobbyController;
        this.gameController = null;
        this.gameState = gameState;
    }

    @Override
    public void onMessageReceived(Message message) {
        if (message == null) {
            System.err.println("Boş mesaj alındı!");
            return;
        }

        try {
            // Mesaj tipine göre işlem yap
            switch (message.getType()) {
                case GAME_STATE:
                    handleGameStateMessage(message);

                    // Sadece süre güncellemesi mi kontrol et
                    Object remainingTimeObj = message.getDataValue("remainingTime");
                    if (remainingTimeObj != null &&
                            message.getDataValue("event") == null &&
                            message.getDataValue("gameOver") == null) {
                        // Sadece süre güncellemesi
                        updateTimeOnly();
                    } else {
                        // TAM OYUN DURUMU GÜNCELLEMESİ
                        System.out.println("ELSE İÇİNDE OLDU BU");
                        updateFullUI();  // Bu metot çağrısı faz değişikliklerinde de olmalı
                    }
                    break;

                case PHASE_CHANGE:  // YENİ EKLENEN
                    System.out.println("PHASE CHANGE'DE OLDU BU");
                    handlePhaseChangeMessage(message);
                    break;

                case PLAYER_JOIN:
                    handlePlayerJoinMessage(message);
                    updatePlayersOnly();
                    break;

                case PLAYER_LEAVE:
                    handlePlayerLeaveMessage(message);
                    updatePlayersOnly();
                    break;

                case ROLE_ASSIGNMENT:
                    handleRoleAssignmentMessage(message);
                    // Rol değiştiğinde UI'ı tam güncelle
                    updateFullUI();
                    break;

                case AVAILABLE_ACTIONS:
                    handleAvailableActionsMessage(message);
                    updateActionsOnly();
                    break;

                case ACTION_RESULT:
                    handleActionResultMessage(message);
                    // Aksiyon sonuçları oyun durumunu etkileyebilir
                    updateFullUI();
                    break;

                case CHAT_MESSAGE:
                    handleChatMessage(message);
                    // Sohbet için UI güncellemesi gerekmez, mesajı sadece ekleriz
                    break;

                case ERROR:
                    handleErrorMessage(message);
                    // Hata mesajları için UI güncellemesi gerekmez
                    break;

                default:
                    System.out.println("Bilinmeyen mesaj tipi: " + message.getType());
                    // Bilinmeyen mesaj tipleri için tam güncelleme yap
                    updateFullUI();
            }
        } catch (Exception e) {
            System.err.println("Mesaj işlenirken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sadece süre güncellemesi yapar
     */
    private void updateTimeOnly() {
        if (gameController != null) {
            gameController.updateTimeOnly();
        }
    }

    /**
     * Sadece oyuncu listesini günceller
     */
    private void updatePlayersOnly() {
        if (gameController != null) {
            gameController.updatePlayerListOnly();
        } else if (lobbyController != null) {
            lobbyController.updatePlayerList();
        }
    }

    /**
     * Sadece aksiyon panelini günceller
     */
    private void updateActionsOnly() {
        if (gameController != null) {
            gameController.updateActionsOnly();
        }
    }

    /**
     * Tam UI güncellemesi yapar
     */
    private void updateFullUI() {
        if (gameController != null) {
            gameController.updateUI();
        } else if (lobbyController != null) {
            lobbyController.updatePlayerList();
        }
    }

    private void updateGamePhase(String newPhaseName, boolean forceUpdate) {
        if (newPhaseName == null) {
            System.out.println("HATA: updateGamePhase'e null faz adı gönderildi!");
            return;
        }

        GameState.Phase oldPhase = gameState.getCurrentPhase();
        GameState.Phase newPhase;

        switch (newPhaseName) {
            case "NIGHT":
                newPhase = GameState.Phase.NIGHT;
                break;
            case "DAY":
                newPhase = GameState.Phase.DAY;
                break;
            default:
                newPhase = GameState.Phase.LOBBY;
        }

        // Faz değiştiyse veya zorunlu güncelleme varsa işle
        if (forceUpdate || oldPhase != newPhase) {
            System.out.println("Faz değişiyor: " + oldPhase + " -> " + newPhase + (forceUpdate ? " (zorla)" : ""));
            gameState.setCurrentPhase(newPhase);

            // UI güncellemesi - Yalnızca UI thread üzerinde!
            if (!Platform.isFxApplicationThread()) {
                final GameState.Phase finalNewPhase = newPhase;
                Platform.runLater(() -> {
                    try {
                        if (gameController != null) {
                            System.out.println("UI thread üzerinde faz gösterimi güncelleniyor: " + finalNewPhase);
                            gameController.updatePhaseDisplay();
                        }
                    } catch (Exception e) {
                        System.err.println("Faz gösterimi güncellenirken hata: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                if (gameController != null) {
                    System.out.println("Zaten UI thread üzerindeyiz, faz gösterimi güncelleniyor: " + newPhase);
                    gameController.updatePhaseDisplay();
                }
            }

            // Faz değişiminden sonra aksiyonları güncellemek için planlama yap
            scheduleActionUpdate();
        } else {
            System.out.println("Faz değişimi atlandı, mevcut faz zaten " + oldPhase);
        }
    }

    private void scheduleActionUpdate() {
        if (gameController == null) return;

        // Zamanlayıcıyı ana JavaFX thread'inde çalıştır
        Platform.runLater(() -> {
            try {
                // Kısa bir gecikme sonra aksiyon panelini güncelleyelim
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> {
                                    try {
                                        System.out.println("Aksiyon paneli gecikmeli güncelleniyor...");
                                        gameController.updateActionsOnly();
                                    } catch (Exception e) {
                                        System.err.println("Aksiyon paneli güncellenirken hata: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }, 500  // 500ms gecikme
                );
            } catch (Exception e) {
                System.err.println("Aksiyon güncellemesi zamanlanırken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleGameStateMessage(Message message) {
        try {
            // YENİ - Faz değişimini atlama bayrağını kontrol et
            Boolean skipPhaseUpdate = (Boolean) message.getDataValue("skipPhaseUpdate");

            // Timestamp bilgisi
            Long timestamp = (Long) message.getDataValue("timestamp");
            System.out.println("Oyun durumu mesajı alındı" +
                    (skipPhaseUpdate != null ? ", skipPhaseUpdate=" + skipPhaseUpdate : "") +
                    (timestamp != null ? ", timestamp=" + timestamp : ""));

            // GameState bilgisini al
            GameStateResponse gameStateResponse = null;
            if (message.getDataValue("gameState") != null) {
                try {
                    String jsonStr = JsonUtils.toJson(message.getDataValue("gameState"));
                    gameStateResponse = JsonUtils.fromJson(jsonStr, GameStateResponse.class);
                    System.out.println("GameStateResponse alındı: phase=" +
                            (gameStateResponse != null ? gameStateResponse.getPhase() : "null"));
                } catch (Exception e) {
                    System.err.println("GameStateResponse işlenirken hata: " + e.getMessage());
                }
            }

            // Faz bilgisini güncelle (SADECE skipPhaseUpdate false ise)
            if (skipPhaseUpdate == null || !skipPhaseUpdate) {
                String phase = null;

                // İlk gameStateResponse'dan faz bilgisini al
                if (gameStateResponse != null) {
                    phase = gameStateResponse.getPhase();
                }

                // Ardından doğrudan mesajdan faz bilgisini al (daha öncelikli)
                if (message.getDataValue("phase") != null) {
                    phase = (String) message.getDataValue("phase");
                }

                if (phase != null) {
                    System.out.println("GAME_STATE mesajından faz güncellemesi: " + phase);
                    // YENİ - Merkezi metodu çağır (zorunlu güncelleme OLMADAN)
                    updateGamePhase(phase, false);
                }
            } else {
                System.out.println("Faz güncellemesi atlanıyor, skipPhaseUpdate=true");
            }

            // Zamanı güncelle
            if (message.getDataValue("remainingTime") != null) {
                Integer time = (Integer) message.getDataValue("remainingTime");
                Integer oldTime = gameState.getRemainingTime();

                // Değişim olduysa güncelle ve logla
                if (time != null && (oldTime == null || !time.equals(oldTime))) {
                    System.out.println("Süre güncelleniyor: " + oldTime + " -> " + time);
                    gameState.setRemainingTime(time);

                    // UI'ı güncelle
                    if (gameController != null) {
                        gameController.updateTimeOnly();
                    }
                }
            }

            // Oyuncu listesini güncelle
            if (message.getDataValue("players") != null) {
                try {
                    List<Map<String, Object>> playerInfosRaw = (List<Map<String, Object>>) message.getDataValue("players");
                    updatePlayerList(playerInfosRaw);
                    System.out.println("Oyun durumunda " + playerInfosRaw.size() + " oyuncu güncellendi");
                } catch (Exception e) {
                    System.err.println("Oyun durumu sırasında oyuncu listesi güncellenirken hata: " + e.getMessage());
                }
            }

            // Özel olayları kontrol et
            String event = (String) message.getDataValue("event");
            if (event != null) {
                System.out.println("Özel olay işleniyor: " + event);
                handleGameEvent(event, message);
            }

            // Sistem mesajını ekle
            String messageText = (String) message.getDataValue("message");
            if (messageText != null) {
                gameState.addSystemMessage(messageText);
                if (gameController != null) {
                    gameController.handleSystemMessage(messageText);
                } else if (lobbyController != null) {
                    lobbyController.addChatMessage(messageText);
                }
            }

            // Oyun durumunu kontrol et
            String state = (String) message.getDataValue("state");
            if (state != null && state.equals("GAME_STARTING") && lobbyController != null) {
                System.out.println("Oyun başlatılıyor...");
                lobbyController.startGame();
            }

            // Oyun sonu kontrolü
            Boolean gameOver = (Boolean) message.getDataValue("gameOver");
            if (gameOver != null && gameOver && gameController != null) {
                String winnerMessage = (String) message.getDataValue("message");
                System.out.println("Oyun sona erdi: " + winnerMessage);
                gameController.handleGameEnd(winnerMessage);
            }

            if (message.getDataValue("players") != null) {
                try {
                    List<Map<String, Object>> playerInfosRaw = (List<Map<String, Object>>) message.getDataValue("players");
                    updatePlayerList(playerInfosRaw);
                    System.out.println("Oyun durumunda " + playerInfosRaw.size() + " oyuncu güncellendi");
                } catch (Exception e) {
                    System.err.println("Oyun durumu sırasında oyuncu listesi güncellenirken hata: " + e.getMessage());
                }
            }

            // Sonra UI'ı güncelleyin
            updateFullUI();

        } catch (Exception e) {
            System.err.println("Oyun durumu mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePhaseChangeMessage(Message message) {
        try {
            // Faz bilgisini al
            String newPhase = (String) message.getDataValue("newPhase");
            if (newPhase == null) {
                System.out.println("UYARI: Geçersiz faz değişim mesajı, 'newPhase' alanı eksik");
                return;
            }

            // Timestamp bilgisi
            Long timestamp = (Long) message.getDataValue("timestamp");
            System.out.println("Faz değişim mesajı alındı: " + newPhase +
                    (timestamp != null ? ", timestamp=" + timestamp : ""));

            // Özel faz değişim flag'ini kontrol et
            Boolean phaseChangeMessage = (Boolean) message.getDataValue("phaseChangeMessage");
            boolean isSpecificPhaseChangeMsg = phaseChangeMessage != null && phaseChangeMessage;

            // Faz değişimlerinde hapis durumunu kontrol et
            GameState.Phase newGamePhase;
            switch (newPhase) {
                case "NIGHT":
                    newGamePhase = GameState.Phase.NIGHT;
                    break;
                case "DAY":
                    newGamePhase = GameState.Phase.DAY;
                    // Gündüz başladıysa, hapis flag'ini temizle - hapsedilme durumu sadece bir gece için geçerli
                    Boolean wasJailed = (Boolean) gameState.getData("isJailed");
                    if (wasJailed != null && wasJailed) {
                        System.out.println("Gündüz fazı başladı, hapis durumu sıfırlanıyor");
                        gameState.setData("isJailed", false);
                    }
                    break;
                default:
                    newGamePhase = GameState.Phase.LOBBY;
            }

            // YENİ - Merkezi metodu çağır (zorla güncelleme ile)
            updateGamePhase(newPhase, true);

            // Sistem mesajı ekle
            String phaseMessage = (String) message.getDataValue("message");
            if (phaseMessage != null) {
                gameState.addSystemMessage(phaseMessage);
                if (gameController != null) {
                    gameController.handleSystemMessage(phaseMessage);
                }
            }

            // Oyuncu listesini güncelle
            if (message.getDataValue("players") != null) {
                try {
                    List<Map<String, Object>> playerInfosRaw = (List<Map<String, Object>>) message.getDataValue("players");
                    updatePlayerList(playerInfosRaw);
                    System.out.println("Faz değişiminde " + playerInfosRaw.size() + " oyuncu güncellendi");
                } catch (Exception e) {
                    System.err.println("Faz değişimi sırasında oyuncu listesi güncellenirken hata: " + e.getMessage());
                }
            }

            // Kalan süre güncelle
            if (message.getDataValue("remainingTime") != null) {
                Integer time = (Integer) message.getDataValue("remainingTime");
                gameState.setRemainingTime(time);
                System.out.println("Faz değişiminde süre güncellendi: " + time);
            }

            // Hapsedilen oyuncu kontrolü
            String jailedPlayer = (String) message.getDataValue("jailedPlayer");
            if (jailedPlayer != null && jailedPlayer.equals(gameState.getCurrentUsername())) {
                gameState.setData("isJailed", true);
                System.out.println("Oyuncu hapsedildi: " + jailedPlayer);
            }

            // UI'ı tam olarak güncelle (önemli)
            updateFullUI();
        } catch (Exception e) {
            System.err.println("Faz değişim mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGameEvent(String event, Message message) {
        switch (event) {
            case "PLAYER_KILLED":
                String target = (String) message.getDataValue("target");
                updatePlayerStatus(target, false);
                break;
            case "PLAYER_EXECUTED":
                String executed = (String) message.getDataValue("target");
                updatePlayerStatus(executed, false);
                break;
            case "PLAYER_PROTECTED":
                // Korunan oyuncu
                String protected_player = (String) message.getDataValue("target");
                gameState.addSystemMessage(protected_player + " korundu!");
                break;
            case "NO_EXECUTION":
                // Kimse asılmadı
                gameState.addSystemMessage("Bugün kimse asılmadı.");
                break;
            case "JAIL_START":
            case "JAILOR_ACTIVE":
                if (gameController != null) {
                    gameController.handleJailEvent(event, message);
                }
                break;
            case "JAILOR_EXECUTION":
                if (gameController != null) {
                    gameController.handleJailEvent(event, message);
                }
                break;
            case "JAIL_PROTECTION":
                // Hapishane koruması için yeni olay
                String message_text = (String) message.getDataValue("message");
                if (message_text != null) {
                    gameState.addSystemMessage(message_text);
                    if (gameController != null) {
                        gameController.handleSystemMessage(message_text);
                    }
                }
                break;
            case "PLAYER_JAILED":
                // Oyuncu hapsedildi
                if (gameState.getCurrentUsername().equals(message.getDataValue("target"))) {
                    // Bu oyuncu hapsedildi, aksiyon durumunu güncelle
                    gameState.setData("isJailed", true);
                    gameState.addSystemMessage("Hapsedildiniz! Gardiyan tarafından sorgulanacaksınız.");
                    System.out.println("Oyuncu hapsedildi, isJailed = true olarak ayarlandı");

                    // Aksiyon panelini temizle
                    if (gameController != null) {
                        gameController.updateActionsOnly();
                    }
                }
                break;

            case "JAIL_END":
                // Hapishane sona erdi, flag'i temizle
                gameState.setData("isJailed", false);
                System.out.println("Hapishane sona erdi, isJailed = false olarak ayarlandı");
                break;
            case "JAILOR_INACTIVE":
                // Eğer bu oyuncu jailor ise ve gündüz birini hapsetmedi
                if (gameState.getCurrentRole().equals("Gardiyan")) {
                    String messageText = (String) message.getDataValue("message");
                    if (messageText != null) {
                        gameState.addSystemMessage(messageText);
                        if (gameController != null) {
                            gameController.handleSystemMessage(messageText);
                        }
                    }

                    // Aksiyon panelinde özel bir mesaj göster, ama paneli gizleme
                    if (gameController != null) {
                        Platform.runLater(() -> {
                            gameController.showInactiveJailorMessage();
                        });
                    }
                }
                break;
            // Diğer olaylar için ek işleme mantığı eklenebilir
        }
    }

    private void handlePlayerJoinMessage(Message message) {
        try {
            String username = null;

            // PlayerJoinResponse'dan değerleri çıkar
            if (message.getDataValue("playerJoin") != null) {
                String jsonStr = JsonUtils.toJson(message.getDataValue("playerJoin"));
                PlayerJoinResponse response = JsonUtils.fromJson(jsonStr, PlayerJoinResponse.class);
                username = response.getUsername();
            } else if (message.getDataValue("username") != null) {
                username = (String) message.getDataValue("username");
            }

            if (username != null) {
                // Oyuncuyu ekle
                Player player = new Player(username);
                gameState.addPlayer(player);

                // Mesajı göster
                String joinMessage = username + " lobiye katıldı!";
                gameState.addSystemMessage(joinMessage);

                if (lobbyController != null) {
                    lobbyController.updatePlayerList();
                    lobbyController.addChatMessage(joinMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Oyuncu katılım mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerLeaveMessage(Message message) {
        try {
            String username = null;

            if (message.getDataValue("playerLeave") != null) {
                String jsonStr = JsonUtils.toJson(message.getDataValue("playerLeave"));
                PlayerLeaveResponse response = JsonUtils.fromJson(jsonStr, PlayerLeaveResponse.class);
                username = response.getUsername();
            } else if (message.getDataValue("username") != null) {
                username = (String) message.getDataValue("username");
            }

            if (username != null) {
                // Oyuncuyu kaldır
                gameState.removePlayer(username);

                // Mesajı göster
                String leaveMessage = username + " lobiden ayrıldı!";
                gameState.addSystemMessage(leaveMessage);

                if (lobbyController != null) {
                    lobbyController.updatePlayerList();
                    lobbyController.addChatMessage(leaveMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Oyuncu ayrılış mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRoleAssignmentMessage(Message message) {
        try {
            String role = null;

            if (message.getDataValue("roleAssignment") != null) {
                String jsonStr = JsonUtils.toJson(message.getDataValue("roleAssignment"));
                RoleAssignmentResponse response = JsonUtils.fromJson(jsonStr, RoleAssignmentResponse.class);
                role = response.getRole();
                System.out.println("DEBUG: Rol responsedan alındı: " + role);
            } else if (message.getDataValue("role") != null) {
                role = (String) message.getDataValue("role");
                System.out.println("DEBUG: Rol doğrudan data'dan alındı: " + role);
            }

            if (role != null) {
                // Rol bilgisini ayarla
                System.out.println("DEBUG: Rol atama başlıyor - Rol: " + role +
                        ", Kullanıcı: " + gameState.getCurrentUsername());
                gameState.setCurrentRole(role);

                // Rol listesini log'a yazdır
                System.out.println("DEBUG: Tüm oyuncular ve rolleri:");
                for (Player p : gameState.getPlayers()) {
                    System.out.println("  - Oyuncu: " + p.getUsername() +
                            ", Rol: " + p.getRole() +
                            ", Hayatta: " + p.isAlive());
                }

                // Mesajı göster
                String roleMessage = "Rolünüz: " + role;
                gameState.addSystemMessage(roleMessage);

                if (gameController != null) {
                    gameController.handleSystemMessage(roleMessage);
                }
            } else {
                System.out.println("UYARI: Rol bilgisi bulunamadı! Mesaj: " + message.toDebugString());
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Rol atama mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAvailableActionsMessage(Message message) {
        try {
            // Kullanılabilir aksiyonları al
            List<String> actions = (List<String>) message.getDataValue("availableActions");
            if (actions != null && !actions.isEmpty()) {
                String availableAction = String.join(", ", actions);
                gameState.setAvailableAction(availableAction);

                String actionMessage = "Kullanılabilir aksiyonlar: " + availableAction;
                gameState.addSystemMessage(actionMessage);

                if (gameController != null) {
                    gameController.handleSystemMessage(actionMessage);
                    gameController.updateActions(actions);
                }
            }
        } catch (Exception e) {
            System.err.println("Kullanılabilir aksiyonlar mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleActionResultMessage(Message message) {
        try {
            if (message.getDataValue("actionResult") != null) {
                String jsonStr = JsonUtils.toJson(message.getDataValue("actionResult"));
                ActionResultResponse response = JsonUtils.fromJson(jsonStr, ActionResultResponse.class);

                if (response != null) {
                    // Aksiyon sonucunu sistem mesajına ekle
                    String actionType = response.getAction();
                    String target = response.getTarget();
                    String result = response.getResult();
                    String resultMessage = response.getMessage();

                    // Özellikle INVESTIGATE aksiyonu için özel mesaj
                    if (ActionType.INVESTIGATE.name().equals(actionType)) {
                        String formattedMessage = String.format("Araştırma Sonucu - %s: %s",
                                target, resultMessage);

                        // Sistem mesajına ekle
                        gameState.addSystemMessage(formattedMessage);

                        // GameController'a ilet
                        if (gameController != null) {
                            gameController.handleSystemMessage(formattedMessage);
                        }

                        System.out.println("Şerif araştırma sonucu eklendi: " + formattedMessage);
                    }
                    // Diğer aksiyon türleri için genel işlem
                    else if (resultMessage != null && !resultMessage.isEmpty()) {
                        // Sistem mesajına ekle
                        gameState.addSystemMessage(resultMessage);

                        // GameController'a ilet
                        if (gameController != null) {
                            gameController.handleSystemMessage(resultMessage);
                        }
                    }

                    // Aksiyonları güncelle
                    List<String> actions = (List<String>) message.getDataValue("availableActions");
                    if (actions != null && !actions.isEmpty()) {
                        if (gameController != null) {
                            // Gecikmeli aksiyon güncellemesi
                            new java.util.Timer().schedule(
                                    new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            Platform.runLater(() -> {
                                                try {
                                                    gameController.updateActions(actions);
                                                } catch (Exception e) {
                                                    System.err.println("Aksiyon güncellemesi sırasında hata: " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            });
                                        }
                                    }, 500  // 500ms gecikme
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Aksiyon sonucu mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChatMessage(Message message) {
        try {
            if (message.getDataValue("chatMessage") != null) {
                String jsonStr = JsonUtils.toJson(message.getDataValue("chatMessage"));
                ChatMessageResponse response = JsonUtils.fromJson(jsonStr, ChatMessageResponse.class);

                if (response != null) {
                    String sender = response.getSender();
                    String content = response.getMessage();
                    String room = response.getRoom();

                    String chatMessage = sender + ": " + content;

                    System.out.println("Sohbet mesajı alındı - Oda: " + room + ", Gönderen: " + sender);

                    if ("MAFIA".equals(room)) {
                        // Mafya mesajı
                        gameState.addMafiaMessage(chatMessage);
                        if (gameController != null) {
                            gameController.handleMafiaMessage(chatMessage);
                        }
                    } else if ("JAIL".equals(room)) {
                        // Hapishane mesajı
                        gameState.addSystemMessage("Hapishane: " + chatMessage);

                        if (gameController != null) {
                            Platform.runLater(() -> {
                                try {
                                    gameController.getView().addJailMessage(chatMessage);

                                    // Tab'ı görünür yap ama otomatik seçme (istemci seçsin)
                                    gameController.getView().ensureJailChatVisible();
                                } catch (Exception e) {
                                    System.err.println("Hapishane mesajı eklenirken hata: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        }
                    } else {
                        // Genel mesaj
                        gameState.addChatMessage(chatMessage);
                        if (gameController != null) {
                            gameController.handleChatMessage(chatMessage);
                        } else if (lobbyController != null) {
                            lobbyController.addChatMessage(chatMessage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Sohbet mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleErrorMessage(Message message) {
        try {
            String code = (String) message.getDataValue("code");
            String errorMessage = (String) message.getDataValue("message");

            String fullError = "HATA: " + (code != null ? code + " - " : "") + errorMessage;
            gameState.addSystemMessage(fullError);

            if (gameController != null) {
                gameController.handleSystemMessage(fullError);
            } else if (lobbyController != null) {
                lobbyController.addChatMessage(fullError);
            }
        } catch (Exception e) {
            System.err.println("Hata mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePlayerList(List<Map<String, Object>> playerInfosRaw) {
        if (playerInfosRaw == null || playerInfosRaw.isEmpty()) {
            System.out.println("UYARI: updatePlayerList'e boş veya null playerInfos gönderildi!");
            return;
        }

        System.out.println("MessageHandler.updatePlayerList() - Oyuncu sayısı: " + playerInfosRaw.size());

        for (Map<String, Object> playerInfoRaw : playerInfosRaw) {
            String username = (String) playerInfoRaw.get("username");
            Boolean alive = (Boolean) playerInfoRaw.get("alive");
            String role = (String) playerInfoRaw.get("role");
            String avatarId = (String) playerInfoRaw.get("avatarId");

            if (username == null) continue;

            System.out.println("  Oyuncu: " + username + ", Avatar: " + avatarId +
                    ", Rol: " + role + ", Hayatta: " + alive);

            Player player = findOrCreatePlayer(username);

            if (alive != null) {
                player.setAlive(alive);
            }

            if (role != null && !role.equals("UNKNOWN")) {
                player.setRole(role);
            }

            if (avatarId != null) {
                player.setAvatarId(avatarId);
                System.out.println("  -> Avatar atandı: " + username + " -> " + avatarId);
            }
        }

        // UI güncelleme
        if (gameController != null || lobbyController != null) {
            Platform.runLater(() -> {
                if (gameController != null) {
                    gameController.updatePlayerListOnly();

                    // Rol avatarını güncelle (sadece kendi rolünü)
                    if (gameState.getCurrentRole() != null) {
                        gameController.getView().updateRoleAvatar(gameState.getCurrentRole());
                    }
                } else if (lobbyController != null) {
                    lobbyController.updatePlayerList();
                }
            });
        }
    }

    private Player findOrCreatePlayer(String username) {
        // Mevcut oyuncuları kontrol et
        for (Player p : gameState.getPlayers()) {
            if (p.getUsername().equals(username)) {
                return p;
            }
        }

        // Oyuncu bulunamadıysa yeni oluştur
        Player newPlayer = new Player(username);
        gameState.addPlayer(newPlayer);
        return newPlayer;
    }

    private void updatePlayerStatus(String username, boolean alive) {
        for (Player p : gameState.getPlayers()) {
            if (p.getUsername().equals(username)) {
                p.setAlive(alive);
                break;
            }
        }
    }

    private void updateUI() {
        Platform.runLater(() -> {
            if (gameController != null) {
                gameController.updateUI();
            } else if (lobbyController != null) {
                lobbyController.updatePlayerList();
            }
        });
    }

    @Override
    public void onConnectionClosed() {
        Platform.runLater(() -> {
            if (gameController != null) {
                gameController.handleDisconnect();
            } else if (lobbyController != null) {
                lobbyController.handleDisconnect();
            }
        });
    }
}