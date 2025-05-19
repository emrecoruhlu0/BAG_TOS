package com.bag_tos;

import com.bag_tos.common.config.GameConfig;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.response.ChatMessageResponse;
import com.bag_tos.common.message.response.GameStateResponse;
import com.bag_tos.common.message.response.PlayerJoinResponse;
import com.bag_tos.common.message.response.PlayerLeaveResponse;
import com.bag_tos.common.model.GamePhase;
import com.bag_tos.common.model.PlayerInfo;

import java.util.*;

/**
 * Odaları ve oyuncuları yöneten sınıf
 */
public class RoomHandler {
    private Map<String, List<ClientHandler>> rooms;
    private List<ClientHandler> players;
    private Game game;

    // Oyun durumu
    private boolean gameStarted;
    private int readyCount;
    private int startCount;

    // Aktif kullanıcı adları
    private Set<String> activeUsernames;

    public RoomHandler() {
        this.rooms = new HashMap<>();
        this.players = new ArrayList<>();
        this.game = new Game(new ArrayList<>());
        this.gameStarted = false;
        this.readyCount = 0;
        this.startCount = 0;
        this.activeUsernames = new HashSet<>();

        // Temel odaları oluştur
        createRoom("LOBBY"); // Ana lobi
        createRoom("MAFYA"); // Mafya özel odası
    }

    public void startGame() {
        if (gameStarted) {
            return; // Zaten başlamış
        }

        gameStarted = true;

        // Oyun başlangıç mesajı
        Message gameStartMessage = new Message(MessageType.GAME_STATE);
        gameStartMessage.addData("phase", GamePhase.LOBBY.name());
        gameStartMessage.addData("message", "Oyun başlıyor! Roller dağıtılıyor...");
        gameStartMessage.addData("state", "GAME_STARTING");

        // Tüm oyunculara bildir
        broadcastToRoom("LOBBY", gameStartMessage);

        // Oyun başlatma işlemleri
        for (ClientHandler player : players) {
            player.setGame(game); // Tüm oyunculara Game referansını ata
        }

        game.setRoomHandler(this);
        game.initializeGame();
        game.start();
    }

    public void createRoom(String roomName) {
        rooms.put(roomName, new ArrayList<>());
    }

    public void createJailRoom(String jailor, String prisoner) {
        String jailRoom = "JAIL_" + jailor; // Bu değişken adı sunucu tarafında kalır, istemciye gönderilmez
        System.out.println("Hapishane odası oluşturuluyor: " + jailRoom + " (Gardiyan: " + jailor + ", Hapsedilen: " + prisoner + ")");

        // Oda yoksa oluştur
        if (!rooms.containsKey(jailRoom)) {
            createRoom(jailRoom);
            System.out.println("Yeni hapishane odası oluşturuldu: " + jailRoom);
        } else {
            System.out.println("Hapishane odası zaten mevcut: " + jailRoom);
        }

        // Oyuncuları odaya ekle
        boolean jailorAdded = false;
        boolean prisonerAdded = false;

        for (ClientHandler p : players) {
            String playerName = p.getUsername();
            if (playerName.equals(jailor)) {
                addToRoom(jailRoom, p);
                jailorAdded = true;
                System.out.println("Gardiyan odaya eklendi: " + jailor);
            } else if (playerName.equals(prisoner)) {
                addToRoom(jailRoom, p);
                prisonerAdded = true;
                System.out.println("Hapsedilen odaya eklendi: " + prisoner);
            }
        }

        if (!jailorAdded) {
            System.out.println("UYARI: Gardiyan bulunamadı: " + jailor);
        }

        if (!prisonerAdded) {
            System.out.println("UYARI: Hapsedilen bulunamadı: " + prisoner);
        }

        // Hapishane başlangıç mesajı - Jailor bilgisini gizleyerek
        Message jailStartMessage = new Message(MessageType.GAME_STATE);
        jailStartMessage.addData("event", "JAIL_START");
        jailStartMessage.addData("message", "Hapishane hücresine hoş geldiniz!");

        // Gardiyan için özel bilgi
        players.stream()
                .filter(p -> p.getUsername().equals(jailor))
                .findFirst()
                .ifPresent(p -> {
                    Message jailorMessage = new Message(MessageType.GAME_STATE);
                    jailorMessage.addData("event", "JAILOR_ACTIVE");
                    jailorMessage.addData("prisoner", prisoner);
                    jailorMessage.addData("message", prisoner + " adlı oyuncuyu hapsettiniz. Gece fazında infaz etmek isterseniz 'İnfaz Et' butonunu kullanabilirsiniz.");
                    jailorMessage.addData("isJailor", true); // Bu flag Gardiyan olduğunu belirtir
                    p.sendJsonMessage(jailorMessage);
                });

        // Hapsedilen için özel bilgi - Jailor ismini vermeden
        players.stream()
                .filter(p -> p.getUsername().equals(prisoner))
                .findFirst()
                .ifPresent(p -> {
                    Message prisonerMessage = new Message(MessageType.GAME_STATE);
                    prisonerMessage.addData("event", "PLAYER_JAILED");
                    prisonerMessage.addData("target", prisoner);
                    prisonerMessage.addData("message", "Bu gece Gardiyan tarafından hapsedildiniz!");
                    prisonerMessage.addData("isPrisoner", true); // Bu flag hapsedilmiş olduğunu belirtir
                    p.sendJsonMessage(prisonerMessage);
                });

        // Sadece hapishane odasına bildirim
        broadcastToRoom(jailRoom, jailStartMessage);
    }
    public void closeJailRoom(String jailor) {
        String jailRoom = "JAIL_" + jailor;

        // Oda kapanış mesajı
        Message jailEndMessage = new Message(MessageType.GAME_STATE);
        jailEndMessage.addData("event", "JAIL_END");
        jailEndMessage.addData("message", "Gece sona erdi, hapishane kapatılıyor!");

        broadcastToRoom(jailRoom, jailEndMessage);

        // Odayı kapat (oyuncuları çıkar)
        List<ClientHandler> playersInRoom = new ArrayList<>(getClientsInRoom(jailRoom));
        for (ClientHandler player : playersInRoom) {
            removeFromRoom(jailRoom, player);
        }
    }

    public void sendJailChatMessage(String jailor, String sender, String message) {
        String jailRoom = "JAIL_" + jailor;
        boolean isJailor = sender.equals(jailor);

        System.out.println("Hapishane mesajı gönderiliyor - Oda: " + jailRoom + ", Gönderen: " + sender);

        // Sohbet mesajı oluştur, gönderen kişinin jailor olup olmadığına göre ismi değiştir
        ChatMessageResponse chatResponse = new ChatMessageResponse(
                isJailor ? "Gardiyan" : sender, // Jailor için sabit "Gardiyan" adını kullan
                message,
                "JAIL"  // JAIL tipinde mesaj olduğunu belirt
        );

        // Sohbet mesajını paketle
        Message chatMessage = new Message(MessageType.CHAT_MESSAGE);
        chatMessage.addData("chatMessage", chatResponse);

        // Hapishane odasındaki tüm oyunculara gönder
        List<ClientHandler> playersInRoom = getClientsInRoom(jailRoom);
        System.out.println("Hapishane mesajı " + playersInRoom.size() + " oyuncuya gönderiliyor");

        broadcastToRoom(jailRoom, chatMessage);
    }

    public void addToRoom(String roomName, ClientHandler player) {
        if (!rooms.containsKey(roomName)) {
            createRoom(roomName);
        }

        // Oyuncu zaten bu odadaysa ekleme
        if (!rooms.get(roomName).contains(player)) {
            rooms.get(roomName).add(player);

            // Eğer lobiye ekliyorsak, oyuncu listesine de ekle
            if (Objects.equals(roomName, "LOBBY")) {
                players.add(player);
                player.setGame(game);
                game.addPlayer(player);
            }

            System.out.println(roomName + " odasına " + player.getUsername() + " girdi");
        }
    }

    public void removeFromRoom(String roomName, ClientHandler player) {
        if (rooms.containsKey(roomName) && rooms.get(roomName).contains(player)) {
            rooms.get(roomName).remove(player);
            System.out.println(roomName + " odasından " + player.getUsername() + " çıkarıldı");
        } else {
            System.out.println("Belirtilen oyuncu odada bulunamadı");
        }
    }

    public void notifyPlayerJoined(String username) {
        // Oyuncu katılım bilgisi
        PlayerJoinResponse joinResponse = new PlayerJoinResponse(
                username,
                players.size()
        );

        // Katılım mesajı
        Message joinMessage = new Message(MessageType.PLAYER_JOIN);
        joinMessage.addData("playerJoin", joinResponse);
        joinMessage.addData("message", username + " lobiye katıldı! (" + players.size() + "/" + GameConfig.MIN_PLAYERS + ")");

        // Tüm oyunculara bildir
        broadcastToRoom("LOBBY", joinMessage);
    }

    public void notifyPlayerLeft(String username) {
        // Oyuncuyu listelerden çıkar
        players.removeIf(p -> p.getUsername().equals(username));
        rooms.forEach((roomName, clientList) ->
                clientList.removeIf(p -> p.getUsername().equals(username))
        );

        // Oyuncu ayrılma bilgisi
        PlayerLeaveResponse leaveResponse = new PlayerLeaveResponse(
                username,
                players.size()
        );

        // Ayrılma mesajı
        Message leaveMessage = new Message(MessageType.PLAYER_LEAVE);
        leaveMessage.addData("playerLeave", leaveResponse);
        leaveMessage.addData("message", username + " lobiden ayrıldı! (" + players.size() + "/" + GameConfig.MIN_PLAYERS + ")");

        // Tüm oyunculara bildir
        broadcastToRoom("LOBBY", leaveMessage);
    }

    public void broadcastChatToRoom(String roomName, String sender, String chatContent, String chatRoom) {
        // Gece fazında genel sohbeti kontrol et
        if (gameStarted && "LOBBY".equals(chatRoom) && game.getCurrentPhase() == GamePhase.NIGHT &&
                GameConfig.DISABLE_CHAT_AT_NIGHT) {
            // Gece fazında mesaj gönderme yetkisi yok, sadece göndericiye hata bildirimi
            Message errorMessage = new Message(MessageType.ERROR);
            errorMessage.addData("code", "NIGHT_CHAT_DISABLED");
            errorMessage.addData("message", "Gece fazında genel sohbette konuşamazsınız!");

            // Sadece göndericiye hata bildir
            players.stream()
                    .filter(p -> p.getUsername().equals(sender))
                    .findFirst()
                    .ifPresent(p -> p.sendJsonMessage(errorMessage));

            return;
        }

        // Sohbet mesajı oluştur
        ChatMessageResponse chatResponse = new ChatMessageResponse(
                sender,
                chatContent,
                chatRoom
        );

        // Sohbet mesajını paketle
        Message chatMessage = new Message(MessageType.CHAT_MESSAGE);
        chatMessage.addData("chatMessage", chatResponse);

        // Odadaki tüm oyunculara bildir
        broadcastToRoom(roomName, chatMessage);
    }

    public void broadcastToRoom(String roomName, Message message) {
        if (rooms.containsKey(roomName)) {
            rooms.get(roomName).forEach(p -> p.sendJsonMessage(message));
        }
    }

    public List<ClientHandler> getClientsInRoom(String roomName) {
        return rooms.getOrDefault(roomName, new ArrayList<>());
    }

    public void increaseReadyCount() {
        readyCount++;
    }

    public void increaseStartCount() {
        startCount++;
    }

    public int getReadyCount() {
        return readyCount;
    }

    public int getStartCount() {
        return startCount;
    }

    public void checkGameStart() {
        if (readyCount >= GameConfig.MIN_PLAYERS && startCount >= 1) {
            startGame();
        } else if (readyCount < GameConfig.MIN_PLAYERS && startCount >= 1) {
            // Yetersiz oyuncu hatası mesajı
            Message errorMessage = new Message(MessageType.ERROR);
            errorMessage.addData("code", "INSUFFICIENT_PLAYERS");
            errorMessage.addData("message", "Oyun başlatmak için en az " + GameConfig.MIN_PLAYERS + " hazır oyuncu gerekiyor");

            // Tüm oyunculara bildir
            broadcastToRoom("LOBBY", errorMessage);
        }
    }

    public boolean isUsernameTaken(String username) {
        return activeUsernames.contains(username);
    }

    public void addUsername(String username) {
        activeUsernames.add(username);
    }

    public void removeUsername(String username) {
        activeUsernames.remove(username);
    }

    public Game getGame() {
        return game;
    }

    public Message createPlayerListMessage() {
        // Oyuncu bilgilerini topla
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        for (ClientHandler player : players) {
            playerInfoList.add(new PlayerInfo(
                    player.getUsername(),
                    player.isAlive(),
                    gameStarted ? "UNKNOWN" : null, // Oyun başlamadıysa rol bilgisi yok
                    player.getAvatarId() // Avatar bilgisini ekle
            ));
        }

        // Oyuncu listesi mesajı
        Message playerListMessage = new Message(MessageType.GAME_STATE);
        playerListMessage.addData("players", playerInfoList);
        playerListMessage.addData("totalPlayers", players.size());
        playerListMessage.addData("state", "LOBBY");

        return playerListMessage;
    }

    public void broadcastGameState() {
        Message gameStateMessage;

        if (gameStarted) {
            // Oyun başlamışsa Game sınıfının durumunu kullan
            gameStateMessage = game.createGameStateMessage();
        } else {
            // Oyun başlamamışsa lobi durumunu göster
            List<PlayerInfo> playerInfoList = new ArrayList<>();
            for (ClientHandler player : players) {
                playerInfoList.add(new PlayerInfo(
                        player.getUsername(),
                        true,
                        null
                ));
            }

            GameStateResponse gameState = new GameStateResponse(
                    GamePhase.LOBBY.name(),
                    0,
                    playerInfoList
            );

            gameStateMessage = new Message(MessageType.GAME_STATE);
            gameStateMessage.addData("gameState", gameState);
            gameStateMessage.addData("readyCount", readyCount);
            gameStateMessage.addData("state", "LOBBY");
        }
        broadcastToRoom("LOBBY", gameStateMessage);
    }
}