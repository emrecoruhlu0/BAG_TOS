package com.bag_tos;

import java.util.*;

import static com.bag_tos.MessageUtils.*;

public class RoomHandler {
    private Map<String, List<ClientHandler>> rooms = new HashMap<>();
    private List<ClientHandler> players = new ArrayList<>();
    Game game = new Game(new ArrayList<>());
    private boolean gameStarted = false;
    int readyCount = 0;
    int startCount = 0;
    ArrayList<String> activeUsernames = new ArrayList<>();

    public RoomHandler() {
        createRoom("LOBBY"); // Ana lobi
        createRoom("MAFYA"); // Mafya özel odası
    }

    public void startGame() {
        gameStarted = true;
        broadcastToRoom("LOBBY","SISTEM: Oyun basliyor! Roller dagitiliyor...");

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

    public void addToRoom(String roomName, ClientHandler player) {
        if (!rooms.containsKey(roomName)) {
            createRoom(roomName);
        }
        // Oyuncu zaten bu odadaysa ekleme
        if (!rooms.get(roomName).contains(player)) {
            rooms.get(roomName).add(player);
            if(Objects.equals(roomName, "LOBBY")){
                players.add(player);
                player.setGame(game);
                game.addPlayer(player);
                broadcastToRoom("LOBBY", "SISTEM: " + player.getUsername() + " lobiye katildi! (" + players.size() + "/4)");
            }
            System.out.println(roomName + " odasına " + player.getUsername() + " girdi");
        }
    }

    public void broadcastToRoom(String roomName, String message) {
        if (rooms.containsKey(roomName)) {
            rooms.get(roomName).forEach(p -> p.sendMessage(message));
        }
    }

    public void removeFromRoom(String roomName, ClientHandler player) {
        if (rooms.get(roomName).contains(player)) {
            rooms.get(roomName).remove(player);
            System.out.println(roomName + " odasından " + player.getUsername() + " çıkarıldı");
        } else{
            System.out.println("öyle biri yok");
        }
    }

    public List<ClientHandler> getClientsInRoom(String roomName) {
        return rooms.getOrDefault(roomName, new ArrayList<>());
    }

    public void readyCountHandle(){
        if(readyCount >= 4 && startCount >= 1){
            startGame();
        }
        else if (readyCount < 4 && startCount >= 1){
            broadcastToRoom("LOBBY", formatError("Yeterli oyuncu yok"));
        }
    }

    public boolean isUsernameTaken(String username) {
        return activeUsernames.contains(username);
    }
    public void addUsername(String username) {
        activeUsernames.add(username);
    }
}