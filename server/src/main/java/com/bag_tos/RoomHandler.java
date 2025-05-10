package com.bag_tos;

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

    // Minimum oyuncu sayısı
    private static final int MIN_PLAYERS = 4;

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
        joinMessage.addData("message", username + " lobiye katıldı! (" + players.size() + "/4)");

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
        leaveMessage.addData("message", username + " lobiden ayrıldı! (" + players.size() + "/4)");

        // Tüm oyunculara bildir
        broadcastToRoom("LOBBY", leaveMessage);
    }

    /**
     * Sohbet mesajını odadaki tüm oyunculara bildirir
     */
    public void broadcastChatToRoom(String roomName, String sender, String chatContent, String chatRoom) {
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
        if (readyCount >= MIN_PLAYERS && startCount >= 1) {
            startGame();
        } else if (readyCount < MIN_PLAYERS && startCount >= 1) {
            // Yetersiz oyuncu hatası mesajı
            Message errorMessage = new Message(MessageType.ERROR);
            errorMessage.addData("code", "INSUFFICIENT_PLAYERS");
            errorMessage.addData("message", "Oyun başlatmak için en az " + MIN_PLAYERS + " hazır oyuncu gerekiyor");

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