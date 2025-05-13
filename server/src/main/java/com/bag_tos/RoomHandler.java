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

    /**
     * RoomHandler oluşturur ve temel odaları kurar
     */
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

    /**
     * Oyunu başlatır
     */
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

    /**
     * Yeni bir oda oluşturur
     */
    public void createRoom(String roomName) {
        rooms.put(roomName, new ArrayList<>());
    }

    public void createJailRoom(String jailor, String prisoner) {
        String jailRoom = "JAIL_" + jailor;

        // Oda yoksa oluştur
        createRoom(jailRoom);

        // Oyuncuları odaya ekle
        players.stream()
                .filter(p -> p.getUsername().equals(jailor) || p.getUsername().equals(prisoner))
                .forEach(p -> addToRoom(jailRoom, p));

        // Hapishane başlangıç mesajı
        Message jailStartMessage = new Message(MessageType.GAME_STATE);
        jailStartMessage.addData("event", "JAIL_START");
        jailStartMessage.addData("message", "Gardiyan hücresine hoş geldiniz!");

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

    /**
     * Oyuncuyu belirtilen odaya ekler
     */
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

    /**
     * Oyuncuyu odadan çıkarır
     */
    public void removeFromRoom(String roomName, ClientHandler player) {
        if (rooms.containsKey(roomName) && rooms.get(roomName).contains(player)) {
            rooms.get(roomName).remove(player);
            System.out.println(roomName + " odasından " + player.getUsername() + " çıkarıldı");
        } else {
            System.out.println("Belirtilen oyuncu odada bulunamadı");
        }
    }

    /**
     * Yeni oyuncu katılımını bildirir
     */
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

    /**
     * Oyuncu ayrılışını bildirir
     */
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

    /**
     * Sohbet mesajını odadaki tüm oyunculara bildirir
     */
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

    /**
     * Mesajı odadaki tüm oyunculara bildirir
     */
    public void broadcastToRoom(String roomName, Message message) {
        if (rooms.containsKey(roomName)) {
            rooms.get(roomName).forEach(p -> p.sendJsonMessage(message));
        }
    }

    /**
     * Odadaki tüm oyuncuları döndürür
     */
    public List<ClientHandler> getClientsInRoom(String roomName) {
        return rooms.getOrDefault(roomName, new ArrayList<>());
    }

    /**
     * Hazır oyuncu sayısını artırır
     */
    public void increaseReadyCount() {
        readyCount++;
    }

    /**
     * Oyun başlatma isteklerini artırır
     */
    public void increaseStartCount() {
        startCount++;
    }

    /**
     * Hazır oyuncu sayısını döndürür
     */
    public int getReadyCount() {
        return readyCount;
    }

    /**
     * Oyun başlatma isteklerini döndürür
     */
    public int getStartCount() {
        return startCount;
    }

    /**
     * Oyun başlatma koşullarını kontrol eder
     */
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

    /**
     * Kullanıcı adının kullanımda olup olmadığını kontrol eder
     */
    public boolean isUsernameTaken(String username) {
        return activeUsernames.contains(username);
    }

    /**
     * Kullanıcı adını aktif listesine ekler
     */
    public void addUsername(String username) {
        activeUsernames.add(username);
    }

    /**
     * Kullanıcı adını aktif listesinden çıkarır
     */
    public void removeUsername(String username) {
        activeUsernames.remove(username);
    }

    /**
     * Oyun nesnesini döndürür
     */
    public Game getGame() {
        return game;
    }

    /**
     * Oyuncuların güncel durumunu içeren mesaj oluşturur
     */
    public Message createPlayerListMessage() {
        // Oyuncu bilgilerini topla
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        for (ClientHandler player : players) {
            playerInfoList.add(new PlayerInfo(
                    player.getUsername(),
                    player.isAlive(),
                    gameStarted ? "UNKNOWN" : null // Oyun başlamadıysa rol bilgisi yok
            ));
        }

        // Oyuncu listesi mesajı
        Message playerListMessage = new Message(MessageType.GAME_STATE);
        playerListMessage.addData("players", playerInfoList);
        playerListMessage.addData("totalPlayers", players.size());
        playerListMessage.addData("state", "LOBBY");

        return playerListMessage;
    }

    /**
     * Oyunun mevcut durumunu bildirir
     */
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

        // Tüm oyunculara bildir
        broadcastToRoom("LOBBY", gameStateMessage);
    }
}