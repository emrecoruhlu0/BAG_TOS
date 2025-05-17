package com.bag_tos.client.view.components;

import com.bag_tos.client.model.Player;
import javafx.application.Platform;
import javafx.scene.layout.Pane;

import java.util.*;

public class PlayerCircleView extends Pane {
    private List<PlayerAvatarView> playerAvatars = new ArrayList<>();
    private double centerX;
    private double centerY;
    private double radius;

    public PlayerCircleView() {
        getStyleClass().add("player-circle-view");

        // Arka plan rengini ayarlayın
        setStyle("-fx-background-color: rgba(200, 200, 200, 0.2);"); // Hafif gri arka plan

        // Minimum boyutu ayarlayın
        setMinSize(300, 300);
        setPrefSize(500, 500);

        // Boyut değişikliklerini dinleyin
        widthProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("PlayerCircleView genişliği değişti: " + oldVal + " -> " + newVal);
            calculatePositions();
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("PlayerCircleView yüksekliği değişti: " + oldVal + " -> " + newVal);
            calculatePositions();
        });

        System.out.println("PlayerCircleView oluşturuldu.");
    }

    public int getPlayerCount() {
        return playerAvatars.size();
    }

    private void calculatePositions() {
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        radius = Math.min(centerX, centerY) * 0.7;

        System.out.println("Yeni çember merkezi: (" + centerX + ", " + centerY + "), radius: " + radius);

        updatePlayerPositions();
    }

    private Map<String, PlayerAvatarView> avatarMap = new HashMap<>();

    // Mevcut sorunlu metod (yaklaşık satır 73 civarında)
// PlayerCircleView.java - Düzeltilmiş versiyon

    public void updatePlayers(List<Player> players) {
        // Mevcut oyuncularla yeni oyuncuları karşılaştır
        if (players == null || players.isEmpty()) {
            System.out.println("UYARI: Boş oyuncu listesi!");
            Platform.runLater(() -> {
                playerAvatars.clear();
                getChildren().clear();
            });
            return;
        }

        // Oyuncu listesini kopyala
        final List<Player> playersCopy = new ArrayList<>(players);

        // Optimizasyon: Oyuncu listesi değişmemiş ise güncelleme yapma
        if (!hasPlayerListChanged(playersCopy)) {
            System.out.println("Oyuncu listesi değişmemiş, güncelleme atlanıyor.");
            return;
        }

        // Yeni avatar listesi oluştur
        List<PlayerAvatarView> newAvatars = new ArrayList<>();

        System.out.println("ÇEMBERE GELEN OYUNCULAR:");
        for (Player p : playersCopy) {
            if (p == null) {
                System.out.println("  > NULL OYUNCU NESNESI!");
                continue;
            }

            System.out.println("  > " + p.getUsername() + " - Avatar: " + p.getAvatarId());

            try {
                PlayerAvatarView avatar = new PlayerAvatarView(p);
                newAvatars.add(avatar);
            } catch (Exception e) {
                System.err.println("Oyuncu avatarı oluşturulurken hata: " + e.getMessage());
            }
        }

        Platform.runLater(() -> {
            try {
                getChildren().clear();
                playerAvatars.clear();
                playerAvatars.addAll(newAvatars);
                getChildren().addAll(newAvatars);
                updatePlayerPositions();

                // Mevcut oyuncu listesini kaydet (optimizasyon için)
                lastPlayerList = new ArrayList<>(playersCopy);
            } catch (Exception e) {
                System.err.println("Oyuncu çemberi güncellenirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private boolean hasPlayerListChanged(List<Player> newPlayers) {
        if (lastPlayerList == null || lastPlayerList.size() != newPlayers.size()) {
            return true;
        }

        // Basit karşılaştırma - oyuncu adları ve durumları değişmiş mi?
        for (int i = 0; i < lastPlayerList.size(); i++) {
            Player oldPlayer = lastPlayerList.get(i);
            Player newPlayer = newPlayers.get(i);

            if (!oldPlayer.getUsername().equals(newPlayer.getUsername()) ||
                    oldPlayer.isAlive() != newPlayer.isAlive() ||
                    !oldPlayer.getAvatarId().equals(newPlayer.getAvatarId())) {
                return true;
            }
        }

        return false;
    }

    // Sınıfa eklenecek değişken
    private List<Player> lastPlayerList = null;

    public void updatePlayerData(List<Player> players) {
        // Önce oyuncu sayısını kontrol edin
        if (players.size() != playerAvatars.size()) {
            // Oyuncu sayısı değiştiyse tüm görünümü yeniden oluşturun
            updatePlayers(players);
            return;
        }

        // Aynı sayıda oyuncu varsa, sadece verileri güncelleyin
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            PlayerAvatarView avatar = playerAvatars.get(i);

            // Oyuncu bilgilerini güncelle
            String username = player.getUsername();
            boolean alive = player.isAlive();
            String avatarId = player.getAvatarId();

            // Avatar görünümünü güncelle
            avatar.updatePlayer(player);
        }
    }


    private void updatePlayerPositions() {
        if (playerAvatars.isEmpty()) return;

        int numPlayers = playerAvatars.size();
        double angleStep = 2 * Math.PI / numPlayers;
        double startAngle = -Math.PI / 2; // Üstten başla

        for (int i = 0; i < numPlayers; i++) {
            PlayerAvatarView avatar = playerAvatars.get(i);

            // Sabit başlangıç açısı kullan, böylece pozisyon tutarlı olur
            double angle = startAngle + i * angleStep;
            double x = centerX + radius * Math.cos(angle) - avatar.getPrefWidth() / 2;
            double y = centerY + radius * Math.sin(angle) - avatar.getPrefHeight() / 2;

            avatar.setLayoutX(x);
            avatar.setLayoutY(y);
        }
    }
}