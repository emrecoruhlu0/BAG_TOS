package com.bag_tos;

import com.bag_tos.common.config.GameConfig;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.request.*;
import com.bag_tos.common.message.response.*;
import com.bag_tos.common.model.ActionType;
import com.bag_tos.common.model.GamePhase;
import com.bag_tos.common.util.JsonUtils;
import com.bag_tos.roles.Role;
import com.bag_tos.roles.mafia.Mafya;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * İstemci bağlantısını yöneten ve JSON mesajlaşma sistemini kullanan sınıf
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private RoomHandler roomHandler;
    private Game game;
    private boolean isAlive = true;

    // Anti-hile
    private int invalidMessageCount = 0;
    private long lastReconnectTime = 0;

    /**
     * Yeni istemci bağlantısı oluşturur
     */
    public ClientHandler(Socket socket, RoomHandler roomHandler) throws IOException {
        this.socket = socket;
        this.roomHandler = roomHandler;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // Karşılama mesajı
            Message welcomeMessage = new Message(MessageType.GAME_STATE);
            welcomeMessage.addData("message", "Hoş geldiniz!");
            welcomeMessage.addData("state", "AUTH");
            sendJsonMessage(welcomeMessage);

            // Kullanıcı adı doğrulama
            String proposedUsername = handleAuthentication();
            if (proposedUsername == null) {
                // Doğrulama başarısız oldu ya da bağlantı kesildi
                return;
            }

            // Kullanıcı adını ayarla ve odaya ekle
            this.username = proposedUsername;
            roomHandler.addUsername(username);
            roomHandler.addToRoom("LOBBY", this);
            this.game = roomHandler.getGame();

            // Yeni oyuncu katıldığını bildir
            roomHandler.notifyPlayerJoined(username);

            // Mesaj döngüsü
            String jsonLine;
            while ((jsonLine = in.readLine()) != null) {
                if (jsonLine.startsWith("{")) {
                    try {
                        Message message = JsonUtils.parseMessage(jsonLine);
                        if (message != null) {
                            processMessage(message);
                        } else {
                            sendErrorMessage("INVALID_FORMAT", "Geçersiz JSON formatı");
                            incrementInvalidMessageCount();
                        }
                    } catch (Exception e) {
                        sendErrorMessage("PARSE_ERROR", "JSON ayrıştırma hatası: " + e.getMessage());
                        incrementInvalidMessageCount();
                    }
                } else {
                    sendErrorMessage("INVALID_FORMAT", "Geçersiz mesaj formatı, JSON bekleniyor");
                    incrementInvalidMessageCount();
                }
            }

        } catch (IOException e) {
            System.out.println("Hata: " + (username != null ? username : "Bilinmeyen kullanıcı") + " bağlantısı kesildi.");
        } finally {
            cleanup();
        }
    }

    /**
     * Geçersiz mesaj sayacını artırır ve gerekirse oyuncuyu atar
     */
    private void incrementInvalidMessageCount() {
        invalidMessageCount++;

        if (invalidMessageCount > GameConfig.MAX_INVALID_MESSAGES) {
            // Oyuncuyu at
            sendErrorMessage("TOO_MANY_INVALID_MESSAGES", "Çok fazla geçersiz mesaj gönderdiniz. Bağlantınız kesilecek.");
            cleanup();
        }
    }

    /**
     * Kullanıcı doğrulama işlemini gerçekleştirir
     * @return Doğrulanmış kullanıcı adı, başarısız olursa null
     */
    private String handleAuthentication() {
        try {
            // Kullanıcı adı isteme mesajı
            Message authRequest = new Message(MessageType.GAME_STATE);
            authRequest.addData("state", "AUTH_REQUEST");
            authRequest.addData("message", "KULLANICI_ADI:");
            sendJsonMessage(authRequest);

            // Kullanıcı adını al ve doğrula
            String proposedUsername = null;
            boolean isUsernameValid = false;

            while (!isUsernameValid) {
                String inputLine = in.readLine();

                if (inputLine == null) {
                    return null; // Bağlantı kesildi
                }

                if (inputLine.startsWith("{")) {
                    // JSON mesajından kullanıcı adı çıkarma
                    try {
                        Message message = JsonUtils.parseMessage(inputLine);
                        if (message != null) {
                            // Farklı mesaj tiplerini kontrol et
                            if (message.getType() == MessageType.READY) {
                                proposedUsername = (String) message.getDataValue("username");
                            } else {
                                // Diğer mesaj tiplerinden username değerini alma
                                proposedUsername = (String) message.getDataValue("username");
                            }
                        }
                    } catch (Exception e) {
                        sendErrorMessage("PARSE_ERROR", "Kullanıcı adı işlenirken hata: " + e.getMessage());
                        continue;
                    }
                } else {
                    // Direkt string olarak kullanıcı adı
                    proposedUsername = inputLine;
                }

                // Kullanıcı adını doğrula
                if (proposedUsername == null || proposedUsername.trim().isEmpty()) {
                    sendErrorMessage("AUTH_ERROR", "Kullanıcı adı boş olamaz!");
                } else if (roomHandler.isUsernameTaken(proposedUsername)) {
                    // Yeniden bağlanma kontrolü
                    if (checkReconnectTimeout()) {
                        sendErrorMessage("AUTH_ERROR", "Bu kullanıcı adı zaten alındı! " +
                                GameConfig.RECONNECT_TIMEOUT_SECONDS + " saniye bekleyin.");
                    } else {
                        sendErrorMessage("AUTH_ERROR", "Bu kullanıcı adı zaten alındı!");
                    }
                } else {
                    isUsernameValid = true;
                }
            }

            return proposedUsername;

        } catch (IOException e) {
            System.err.println("Doğrulama sırasında hata: " + e.getMessage());
            return null;
        }
    }

    /**
     * Yeniden bağlanma zaman aşımını kontrol eder
     */
    private boolean checkReconnectTimeout() {
        long currentTime = System.currentTimeMillis() / 1000; // Saniye cinsinden mevcut zaman

        if (lastReconnectTime > 0 &&
                (currentTime - lastReconnectTime) < GameConfig.RECONNECT_TIMEOUT_SECONDS) {
            return true; // Zaman aşımı henüz dolmadı
        }

        // Yeni zaman kaydı
        lastReconnectTime = currentTime;
        return false;
    }

    /**
     * Gelen JSON mesajını işler
     */
    private void processMessage(Message message) {
        try {
            // Oyuncu ölü ise, sadece belirli mesajlara izin ver
            if (!isAlive && message.getType() != MessageType.CHAT) {
                if (!GameConfig.ALLOW_DEAD_CHAT) {
                    sendErrorMessage("FORBIDDEN", "Ölüsünüz, işlem yapamazsınız!");
                    return;
                }
            }

            // Mesaj tipine göre işle
            switch (message.getType()) {
                case READY:
                    handleReadyCommand(message);
                    break;

                case START_GAME:
                    handleStartGameCommand(message);
                    break;

                case ACTION:
                    handleActionCommand(message);
                    break;

                case VOTE:
                    handleVoteCommand(message);
                    break;

                case CHAT:
                    handleChatCommand(message);
                    break;

                default:
                    sendErrorMessage("UNKNOWN_MESSAGE", "Bilinmeyen mesaj tipi: " + message.getType());
            }

        } catch (Exception e) {
            System.err.println("Mesaj işlenirken hata: " + e.getMessage());
            e.printStackTrace();
            sendErrorMessage("PROCESSING_ERROR", "Mesaj işlenirken bir hata oluştu: " + e.getMessage());
        }
    }

    /**
     * Hazır olma mesajını işler
     */
    private void handleReadyCommand(Message message) {
        try {
            ReadyRequest readyRequest = JsonUtils.fromJson(
                    JsonUtils.toJson(message.getDataValue("readyRequest")),
                    ReadyRequest.class
            );

            if (readyRequest != null && readyRequest.isReady()) {
                roomHandler.increaseReadyCount();
                System.out.println("Ready isteği: " + roomHandler.getReadyCount());
                roomHandler.checkGameStart();

                // Başarılı işlem bildirimi
                Message response = new Message(MessageType.GAME_STATE);
                response.addData("status", "success");
                response.addData("message", "Hazır durumundasınız");
                response.addData("readyCount", roomHandler.getReadyCount());
                sendJsonMessage(response);

                // Tüm oyunculara güncel durum bildir
                roomHandler.broadcastGameState();
            }
        } catch (Exception e) {
            System.err.println("Ready komutu işlenirken hata: " + e.getMessage());
            sendErrorMessage("PROCESSING_ERROR", "Ready komutu işlenirken hata oluştu");
        }
    }

    /**
     * Oyun başlatma mesajını işler
     */
    private void handleStartGameCommand(Message message) {
        try {
            roomHandler.increaseStartCount();
            System.out.println("Start isteği: " + roomHandler.getStartCount());
            roomHandler.checkGameStart();

            // Başarılı işlem bildirimi
            Message response = new Message(MessageType.GAME_STATE);
            response.addData("status", "success");
            response.addData("message", "Oyun başlatma isteği gönderildi");
            response.addData("startCount", roomHandler.getStartCount());
            sendJsonMessage(response);
        } catch (Exception e) {
            System.err.println("Start komutu işlenirken hata: " + e.getMessage());
            sendErrorMessage("PROCESSING_ERROR", "Start komutu işlenirken hata oluştu");
        }
    }

    /**
     * Aksiyon mesajını işler
     */
    /**
     * Aksiyon mesajını işler
     */
    private void handleActionCommand(Message message) {
        try {
            ActionRequest actionRequest = JsonUtils.fromJson(
                    JsonUtils.toJson(message.getDataValue("actionRequest")),
                    ActionRequest.class
            );

            if (actionRequest != null) {
                String actionTypeStr = actionRequest.getActionType();
                String target = actionRequest.getTarget();

                try {
                    ActionType actionType = ActionType.valueOf(actionTypeStr);

                    // Kendi üzerinde aksiyon kontrolü
                    if (!GameConfig.ALLOW_SELF_ACTIONS && target.equals(username)) {
                        sendErrorMessage("FORBIDDEN", "Kendiniz üzerinde aksiyon yapamazsınız!");
                        return;
                    }

                    // Mevcut faz kontrolü ve aksiyonların işlenmesi
                    if (game.getCurrentPhase() == GamePhase.NIGHT) {
                        // Gece aksiyonları
                        if (actionType == ActionType.KILL || actionType == ActionType.HEAL ||
                                actionType == ActionType.INVESTIGATE) {

                            // Rol yetkisi kontrolü
                            Role role = game.getRole(username);
                            boolean authorized = false;

                            if (actionType == ActionType.KILL && role instanceof Mafya) {
                                authorized = true;
                            } else if (actionType == ActionType.HEAL && role.getRoleType() == RoleType.DOKTOR) {
                                authorized = true;
                            } else if (actionType == ActionType.INVESTIGATE && role.getRoleType() == RoleType.SERIF) {
                                authorized = true;
                            }

                            if (!authorized) {
                                sendErrorMessage("UNAUTHORIZED", "Bu aksiyonu gerçekleştirme yetkiniz yok");
                                return;
                            }

                            // Aksiyonu kaydet
                            game.registerNightAction(username, actionType, target);

                            // Başarılı işlem bildirimi
                            ActionResultResponse resultResponse = new ActionResultResponse(
                                    actionTypeStr, target, "SUCCESS", "Aksiyon başarıyla gerçekleştirildi"
                            );

                            Message resultMessage = new Message(MessageType.ACTION_RESULT);
                            resultMessage.addData("actionResult", resultResponse);

                            sendJsonMessage(resultMessage);
                        } else {
                            sendErrorMessage("INVALID_PHASE", "Bu aksiyonu şu anda gerçekleştiremezsiniz");
                        }
                    } else if (game.getCurrentPhase() == GamePhase.DAY) {
                        // Gündüz aksiyonları
                        if (actionType == ActionType.VOTE) {
                            game.registerVote(username, target);

                            // Başarılı işlem bildirimi
                            ActionResultResponse resultResponse = new ActionResultResponse(
                                    "VOTE", target, "SUCCESS", "Oy başarıyla kullanıldı"
                            );

                            Message resultMessage = new Message(MessageType.ACTION_RESULT);
                            resultMessage.addData("actionResult", resultResponse);

                            sendJsonMessage(resultMessage);
                        } else {
                            sendErrorMessage("INVALID_PHASE", "Bu aksiyonu şu anda gerçekleştiremezsiniz");
                        }
                    }

                    // BURAYA EKLEYİN: Jailor aksiyonları için olan kod
                    // Jailor aksiyonları için
                    if (actionType == ActionType.JAIL && game.getCurrentPhase() == GamePhase.DAY) {
                        Role role = game.getRole(username);
                        if (role != null && role.getRoleType() == RoleType.JAILOR) {
                            game.registerJailAction(username, target);

                            // Başarılı işlem bildirimi
                            ActionResultResponse resultResponse = new ActionResultResponse(
                                    ActionType.JAIL.name(),
                                    target,
                                    "SUCCESS",
                                    "Hapsetme işlemi kaydedildi. Gece fazında etkili olacak."
                            );

                            Message resultMessage = new Message(MessageType.ACTION_RESULT);
                            resultMessage.addData("actionResult", resultResponse);

                            sendJsonMessage(resultMessage);
                        } else {
                            sendErrorMessage("UNAUTHORIZED", "Bu aksiyonu gerçekleştirme yetkiniz yok");
                        }
                        return;
                    }

                    // İnfaz aksiyonu için
                    if (actionType == ActionType.EXECUTE && game.getCurrentPhase() == GamePhase.NIGHT) {
                        Role role = game.getRole(username);
                        if (role != null && role.getRoleType() == RoleType.JAILOR) {
                            // Aksiyonu kaydet
                            Map<ActionType, String> playerActions = game.getNightActions().computeIfAbsent(username, k -> new HashMap<>());
                            playerActions.put(actionType, target);

                            // Başarılı işlem bildirimi
                            ActionResultResponse resultResponse = new ActionResultResponse(
                                    ActionType.EXECUTE.name(),
                                    target,
                                    "SUCCESS",
                                    "İnfaz işlemi kaydedildi. Gece sonunda etkili olacak."
                            );

                            Message resultMessage = new Message(MessageType.ACTION_RESULT);
                            resultMessage.addData("actionResult", resultResponse);

                            sendJsonMessage(resultMessage);
                        } else {
                            sendErrorMessage("UNAUTHORIZED", "Bu aksiyonu gerçekleştirme yetkiniz yok");
                        }
                        return;
                    }

                } catch (IllegalArgumentException e) {
                    sendErrorMessage("INVALID_ACTION_TYPE", "Geçersiz aksiyon tipi: " + actionTypeStr);
                }
            }
        } catch (Exception e) {
            System.err.println("Aksiyon komutu işlenirken hata: " + e.getMessage());
            sendErrorMessage("PROCESSING_ERROR", "Aksiyon komutu işlenirken hata oluştu");
        }
    }
    /**
     * Oylama mesajını işler
     */
    private void handleVoteCommand(Message message) {
        try {
            VoteRequest voteRequest = JsonUtils.fromJson(
                    JsonUtils.toJson(message.getDataValue("voteRequest")),
                    VoteRequest.class
            );

            if (voteRequest != null) {
                String target = voteRequest.getTarget();

                // Kendi kendine oy kontrolü
                if (!GameConfig.ALLOW_SELF_ACTIONS && target.equals(username)) {
                    sendErrorMessage("FORBIDDEN", "Kendinize oy veremezsiniz!");
                    return;
                }

                if (game.getCurrentPhase() == GamePhase.DAY) {
                    game.registerVote(username, target);

                    // Başarılı işlem bildirimi
                    ActionResultResponse resultResponse = new ActionResultResponse(
                            "VOTE", target, "SUCCESS", "Oy başarıyla kullanıldı"
                    );

                    Message resultMessage = new Message(MessageType.ACTION_RESULT);
                    resultMessage.addData("actionResult", resultResponse);

                    sendJsonMessage(resultMessage);
                } else {
                    sendErrorMessage("INVALID_PHASE", "Oylamayı şu anda gerçekleştiremezsiniz");
                }
            }
        } catch (Exception e) {
            System.err.println("Oylama komutu işlenirken hata: " + e.getMessage());
            sendErrorMessage("PROCESSING_ERROR", "Oylama komutu işlenirken hata oluştu");
        }
    }

    /**
     * Sohbet mesajını işler
     */
    private void handleChatCommand(Message message) {
        try {
            ChatRequest chatRequest = JsonUtils.fromJson(
                    JsonUtils.toJson(message.getDataValue("chatRequest")),
                    ChatRequest.class
            );

            if (chatRequest != null) {
                String chatMessage = chatRequest.getMessage();
                String room = chatRequest.getRoom();

                // Ölü oyuncular için sohbet kontrolü
                if (!isAlive && !GameConfig.ALLOW_DEAD_CHAT) {
                    sendErrorMessage("FORBIDDEN", "Ölü oyuncular sohbet edemez!");
                    return;
                }

                // Gece fazı kontrolü
                if (room.equals("LOBBY") && game.getCurrentPhase() == GamePhase.NIGHT &&
                        GameConfig.DISABLE_CHAT_AT_NIGHT) {
                    sendErrorMessage("NIGHT_CHAT_DISABLED", "Gece fazında genel sohbette konuşamazsınız!");
                    return;
                }

                if ("MAFIA".equals(room) && isMafia()) {
                    // Mafya sohbeti
                    roomHandler.broadcastChatToRoom("MAFYA", username, chatMessage, "MAFIA");
                } else if (game.getCurrentPhase() == GamePhase.DAY || game.getCurrentPhase() == GamePhase.LOBBY || "LOBBY".equals(room)) {
                    // Genel sohbet
                    roomHandler.broadcastChatToRoom("LOBBY", username, chatMessage, "LOBBY");
                } else {
                    sendErrorMessage("INVALID_PHASE", "Şu anda mesaj gönderemezsiniz");
                }
            }
        } catch (Exception e) {
            System.err.println("Sohbet komutu işlenirken hata: " + e.getMessage());
            sendErrorMessage("PROCESSING_ERROR", "Sohbet komutu işlenirken hata oluştu");
        }
    }

    /**
     * Kaynakları temizler ve bağlantıyı kapatır
     */
    void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (username != null) {
                roomHandler.removeUsername(username);
                roomHandler.notifyPlayerLeft(username);
            }
        } catch (IOException e) {
            System.err.println("Temizleme sırasında hata: " + e.getMessage());
        }
    }

    /**
     * JSON formatında mesaj gönderir
     */
    public void sendJsonMessage(Message message) {
        String jsonString = JsonUtils.toJson(message);
        out.println(jsonString);
    }

    /**
     * Hata mesajı oluşturur ve gönderir
     */
    private void sendErrorMessage(String code, String errorMessage) {
        Message errorMsg = new Message(MessageType.ERROR);
        errorMsg.addData("code", code);
        errorMsg.addData("message", errorMessage);

        sendJsonMessage(errorMsg);
    }

    // Getter ve Setter metodları

    /**
     * Oyuncunun hayatta olup olmadığını ayarlar
     */
    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    /**
     * Oyuncunun kullanıcı adını döndürür
     */
    public String getUsername() {
        return username;
    }

    /**
     * Oyun referansını ayarlar
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Oyun referansını döndürür
     */
    public Game getGame() {
        return game;
    }

    /**
     * Oyuncunun hayatta olup olmadığını döndürür
     */
    public boolean isAlive() {
        return isAlive;
    }

    /**
     * Oyuncunun Mafya olup olmadığını kontrol eder
     */
    private boolean isMafia() {
        return game != null && game.getRole(getUsername()) instanceof Mafya;
    }
}