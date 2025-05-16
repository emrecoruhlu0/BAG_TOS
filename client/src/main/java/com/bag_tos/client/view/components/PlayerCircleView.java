package com.bag_tos.client.view.components;

import com.bag_tos.client.model.Player;
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

    // Test kenarlığını kaldır
// playerCircleView.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

// Daha iyi ölçeklendirme için radius hesaplamasını güncelle
    //radius = Math.min(centerX, centerY) * 0.6; // 0.7 yerine 0.6 kullanılabilir

    // avatarMap kullanımını etkinleştir
    public void updatePlayers(List<Player> players) {
        // Mevcut tüm avatarları temizle
        getChildren().clear();
        playerAvatars.clear();

        // Hata kontrolü
        if (players == null || players.isEmpty()) {
            System.out.println("UYARI: Boş oyuncu listesi!");
            return;
        }

        // Debug log: Gelen oyuncuları ve avatarlarını göster
        System.out.println("ÇEMBERE GELEN OYUNCULAR:");
        for (Player p : players) {
            // Null kontrolleri ekle
            if (p == null) {
                System.out.println("  > NULL OYUNCU NESNESI!");
                continue;
            }

            System.out.println("  > " + p.getUsername() + " - Avatar: " + p.getAvatarId());

            try {
                PlayerAvatarView avatar = new PlayerAvatarView(p);
                playerAvatars.add(avatar);
                getChildren().add(avatar);
            } catch (Exception e) {
                System.err.println("Oyuncu avatarı oluşturulurken hata: " + e.getMessage());
            }
        }

        // Pozisyonları güncelle
        updatePlayerPositions();
    }

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