package com.bag_tos.client.network;

import com.bag_tos.client.controller.GameController;
import com.bag_tos.client.controller.LobbyController;
import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.response.*;
import com.bag_tos.common.model.PlayerInfo;
import com.bag_tos.common.util.JsonUtils;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;

public class MessageHandler implements NetworkManager.MessageListener {
    private GameController gameController;
    private LobbyController lobbyController;
    private GameState gameState;

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
                        updateFullUI();  // Bu metot çağrısı faz değişikliklerinde de olmalı
                    }
                    break;

                case PHASE_CHANGE:  // YENİ EKLENEN
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

    private void handleGameStateMessage(Message message) {
        try {
            // GameState bilgisini al
            GameStateResponse gameStateResponse = null;
            if (message.getDataValue("gameState") != null) {
                String jsonStr = JsonUtils.toJson(message.getDataValue("gameState"));
                gameStateResponse = JsonUtils.fromJson(jsonStr, GameStateResponse.class);
            }

            // Faz bilgisini güncelle - BU KISIM ÖNEMLİ
            String phase = (String) message.getDataValue("phase");
            if (phase != null) {
                System.out.println("Sunucudan faz güncellemesi: " + phase);
                switch (phase) {
                    case "NIGHT":
                        gameState.setCurrentPhase(GameState.Phase.NIGHT);
                        break;
                    case "DAY":
                        gameState.setCurrentPhase(GameState.Phase.DAY);
                        break;
                    case "LOBBY":
                    default:
                        gameState.setCurrentPhase(GameState.Phase.LOBBY);
                        break;
                }
            }

            // Zamanı güncelle
            if (message.getDataValue("remainingTime") != null) {
                Integer time = (Integer) message.getDataValue("remainingTime");
                gameState.setRemainingTime(time);
            }

            // Oyuncu listesini güncelle
            if (gameStateResponse != null && gameStateResponse.getPlayers() != null) {
                updatePlayerList(gameStateResponse.getPlayers());
            } else if (message.getDataValue("players") != null) {
                List<Map<String, Object>> playerInfosRaw = (List<Map<String, Object>>) message.getDataValue("players");
                for (Map<String, Object> playerInfoRaw : playerInfosRaw) {
                    String username = (String) playerInfoRaw.get("username");
                    Boolean alive = (Boolean) playerInfoRaw.get("alive");
                    String role = (String) playerInfoRaw.get("role");

                    Player player = findOrCreatePlayer(username);
                    if (alive != null) player.setAlive(alive);
                    if (role != null && !role.equals("UNKNOWN")) player.setRole(role);
                }
            }

            // Özel olayları kontrol et
            String event = (String) message.getDataValue("event");
            if (event != null) {
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
                lobbyController.startGame();

                // Oyun başlatıldığında kısa bir gecikme ile aksiyonları güncelle
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> {
                                    // GameController oluşturuldu mu kontrol et
                                    if (gameController != null) {
                                        // Debug log
                                        System.out.println("Oyun başladı - aksiyonları güncellemek için zamanlı görev çalışıyor");
                                        System.out.println("Oyuncu listesi boyutu: " + gameState.getPlayers().size());

                                        // Önce tam güncelleme yap
                                        gameController.updateUI();
                                        // Sonra özellikle aksiyonları güncelle
                                        gameController.updateActionsOnly();
                                    }
                                });
                            }
                        },
                        1000 // 1 saniye gecikme
                );
            }

            // Oyun sonu kontrolü
            Boolean gameOver = (Boolean) message.getDataValue("gameOver");
            if (gameOver != null && gameOver && gameController != null) {
                String winnerMessage = (String) message.getDataValue("message");
                gameController.handleGameEnd(winnerMessage);
            }

        } catch (Exception e) {
            System.err.println("Oyun durumu mesajı işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePhaseChangeMessage(Message message) {
        // Faz bilgisini al
        String newPhase = (String) message.getDataValue("newPhase");
        if (newPhase == null) return;

        // Debug log
        System.out.println("Faz değişim mesajı alındı: " + newPhase);

        // GameState'i güncelle
        boolean fazDegisti = false;

        if ("NIGHT".equals(newPhase) && gameState.getCurrentPhase() != GameState.Phase.NIGHT) {
            gameState.setCurrentPhase(GameState.Phase.NIGHT);
            fazDegisti = true;
        } else if ("DAY".equals(newPhase) && gameState.getCurrentPhase() != GameState.Phase.DAY) {
            gameState.setCurrentPhase(GameState.Phase.DAY);
            fazDegisti = true;
        } else if ("LOBBY".equals(newPhase) && gameState.getCurrentPhase() != GameState.Phase.LOBBY) {
            gameState.setCurrentPhase(GameState.Phase.LOBBY);
            fazDegisti = true;
        }

        // Sistem mesajı ekle
        String phaseMessage = (String) message.getDataValue("message");
        if (phaseMessage != null) {
            gameState.addSystemMessage(phaseMessage);
            if (gameController != null) {
                gameController.handleSystemMessage(phaseMessage);
            }
        }

        // Faz değiştiyse UI'ı tam güncelle
        if (fazDegisti) {
            updateFullUI();
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
            } else if (message.getDataValue("role") != null) {
                role = (String) message.getDataValue("role");
            }

            if (role != null) {
                // Rolü ayarla
                gameState.setCurrentRole(role);

                // Mesajı göster
                String roleMessage = "Rolünüz: " + role;
                gameState.addSystemMessage(roleMessage);

                if (gameController != null) {
                    gameController.handleSystemMessage(roleMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Rol atama mesajı işlenirken hata: " + e.getMessage());
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
                    String actionMessage = "Aksiyon sonucu: " + response.getMessage();
                    gameState.addSystemMessage(actionMessage);

                    if (gameController != null) {
                        gameController.handleSystemMessage(actionMessage);
                    }

                    // Öldürüldüyse durumu güncelle
                    if ("KILLED".equals(response.getResult()) || "EXECUTED".equals(response.getResult())) {
                        if (response.getTarget() != null) {
                            updatePlayerStatus(response.getTarget(), false);
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

                    if ("MAFIA".equals(room)) {
                        // Mafya mesajı
                        gameState.addMafiaMessage(chatMessage);
                        if (gameController != null) {
                            gameController.handleMafiaMessage(chatMessage);
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

    private void updatePlayerList(List<PlayerInfo> playerInfos) {
        if (playerInfos == null) return;

        for (PlayerInfo info : playerInfos) {
            Player player = findOrCreatePlayer(info.getUsername());
            player.setAlive(info.isAlive());
            if (info.getRole() != null && !info.getRole().equals("UNKNOWN")) {
                player.setRole(info.getRole());
            }
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