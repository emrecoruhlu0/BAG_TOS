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
    private List<Player> lastPlayerList = null;
    private boolean isLayoutInitialized = false;

    public PlayerCircleView() {
        getStyleClass().add("player-circle-view");

        // Belirgin bir arka plan rengi ekle (debug için)
        setStyle("-fx-background-color: rgba(200, 200, 200, 0.2); -fx-border-color: #cccccc; -fx-border-width: 1px;");

        // Minimum boyut ayarla
        setMinSize(300, 300);
        setPrefSize(500, 500);

        // Boyut değişikliğini dinle
        widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                recalculateLayout();
            }
        });

        heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                recalculateLayout();
            }
        });

        // Parent'a eklendiğinde
        parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                // Parent eklendiğinde bir tick sonra layout'u hesapla
                Platform.runLater(this::recalculateLayout);
            }
        });

        System.out.println("PlayerCircleView oluşturuldu: " + getWidth() + "x" + getHeight());
    }

    @Override
    public void layoutChildren() {
        super.layoutChildren();

        if (!isLayoutInitialized && getWidth() > 0 && getHeight() > 0) {
            recalculateLayout();
            isLayoutInitialized = true;
        }
    }

    private void recalculateLayout() {
        double width = getWidth();
        double height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        centerX = width / 2;
        centerY = height / 2;
        radius = Math.min(centerX, centerY) * 0.7;

        System.out.println("Çember yeniden hesaplandı - Boyut: " + width + "x" + height +
                ", Merkez: (" + centerX + ", " + centerY + "), Yarıçap: " + radius);

        updatePlayerPositions();
    }

    public void updatePlayers(List<Player> players) {
        // Boş liste kontrolü
        if (players == null || players.isEmpty()) {
            System.out.println("UYARI: PlayerCircleView.updatePlayers - Boş oyuncu listesi!");
            Platform.runLater(() -> {
                getChildren().clear();
                playerAvatars.clear();
                lastPlayerList = new ArrayList<>();
            });
            return;
        }

        // Değişikliği tam olarak algılamak için oyuncuları derin kopyala
        final List<Player> playersCopy = deepCopyPlayers(players);

        // Değişiklik olup olmadığını kontrol et
        if (!hasPlayerListChanged(playersCopy)) {
            System.out.println("PlayerCircleView - Oyuncu listesi değişmemiş, güncelleme atlanıyor.");
            return;
        }

        System.out.println("PlayerCircleView.updatePlayers - " + playersCopy.size() + " oyuncu güncelleniyor");

        Platform.runLater(() -> {
            try {
                // Tüm çocukları ve avatarları temizle
                getChildren().clear();
                playerAvatars.clear();

                // Her oyuncu için avatar oluştur
                for (Player player : playersCopy) {
                    try {
                        PlayerAvatarView avatarView = new PlayerAvatarView(player);
                        playerAvatars.add(avatarView);
                        getChildren().add(avatarView);

                        System.out.println("Avatar eklendi: " + player.getUsername() +
                                ", Avatar ID: " + player.getAvatarId() +
                                ", Hayatta: " + player.isAlive());
                    } catch (Exception e) {
                        System.err.println("Avatar oluşturulurken hata: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Pozisyonları hesapla
                updatePlayerPositions();

                // Son listeyi güncelle
                lastPlayerList = playersCopy;

                System.out.println("PlayerCircleView - Toplam " + playerAvatars.size() +
                        " avatar eklendi, çemberde görüntüleniyor.");

            } catch (Exception e) {
                System.err.println("PlayerCircleView.updatePlayers - Hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private List<Player> deepCopyPlayers(List<Player> players) {
        List<Player> result = new ArrayList<>();
        for (Player player : players) {
            Player copy = new Player(
                    player.getUsername(),
                    player.getRole(),
                    player.isAlive(),
                    player.getAvatarId()
            );
            result.add(copy);
        }
        return result;
    }

    private boolean hasPlayerListChanged(List<Player> newPlayers) {
        if (lastPlayerList == null || lastPlayerList.size() != newPlayers.size()) {
            return true;
        }

        for (int i = 0; i < lastPlayerList.size(); i++) {
            Player oldPlayer = lastPlayerList.get(i);
            Player newPlayer = newPlayers.get(i);

            if (!Objects.equals(oldPlayer.getUsername(), newPlayer.getUsername()) ||
                    oldPlayer.isAlive() != newPlayer.isAlive() ||
                    !Objects.equals(oldPlayer.getAvatarId(), newPlayer.getAvatarId())) {
                return true;
            }
        }
        return false;
    }

    private void updatePlayerPositions() {
        if (playerAvatars.isEmpty() || centerX <= 0 || centerY <= 0 || radius <= 0) {
            return;
        }

        int numPlayers = playerAvatars.size();
        double angleStep = 2 * Math.PI / numPlayers;
        double startAngle = -Math.PI / 2; // Üstten başla

        System.out.println("Oyuncular konumlandırılıyor - Oyuncu sayısı: " + numPlayers +
                ", Merkez: (" + centerX + "," + centerY + "), Yarıçap: " + radius);

        for (int i = 0; i < numPlayers; i++) {
            PlayerAvatarView avatar = playerAvatars.get(i);

            // Avatarın görünür boyutlarını al
            double avatarWidth = avatar.getPrefWidth();
            double avatarHeight = avatar.getPrefHeight();

            if (avatarWidth <= 0) avatarWidth = 100; // Varsayılan değerler
            if (avatarHeight <= 0) avatarHeight = 120;

            // Çember üzerinde konumlandır
            double angle = startAngle + i * angleStep;
            double x = centerX + radius * Math.cos(angle) - avatarWidth / 2;
            double y = centerY + radius * Math.sin(angle) - avatarHeight / 2;

            // Pozisyonu ayarla
            avatar.setLayoutX(x);
            avatar.setLayoutY(y);

            // Debug bilgisi
            System.out.println("Avatar konumlandırıldı - " + avatar.getUsername() +
                    " -> (" + x + "," + y + ")");
        }
    }
}