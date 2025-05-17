package com.bag_tos.client.view.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.bag_tos.client.model.Player;
import java.io.InputStream;

public class PlayerAvatarView extends VBox {
    private ImageView avatarImage;
    private ImageView deadOverlay;
    private StackPane avatarContainer;
    private Label nameLabel;
    private Player player;
    private final String username;
    private final String avatarId;

    public PlayerAvatarView(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        this.player = player;
        this.username = player.getUsername();
        this.avatarId = player.getAvatarId() != null ? player.getAvatarId() : "avatar1";

        setAlignment(Pos.CENTER);
        setSpacing(5);
        setPrefSize(100, 120);
        setMinSize(80, 100);
        getStyleClass().add("player-avatar-view");

        // Avatar görüntüsü oluştur
        avatarImage = new ImageView();
        avatarImage.setFitWidth(80);
        avatarImage.setFitHeight(80);
        avatarImage.setPreserveRatio(true);

        // Ölüm overlay'i
        deadOverlay = new ImageView();
        deadOverlay.setFitWidth(80);
        deadOverlay.setFitHeight(80);
        deadOverlay.setPreserveRatio(true);
        deadOverlay.setVisible(false);

        // Avatar arkaplan konteynerı
        avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("avatar-container");
        avatarContainer.getChildren().add(avatarImage);

        // İsim etiketi
        nameLabel = new Label(username);
        nameLabel.getStyleClass().add("player-name-label");

        // Düzen oluştur
        getChildren().addAll(avatarContainer, nameLabel);

        // Tooltip ekle (bilgi balonu)
        Tooltip tooltip = new Tooltip(username);
        Tooltip.install(this, tooltip);

        // Görüntüyü yükle
        loadAvatar();

        // Durum güncelle
        updateStyle();

        System.out.println("PlayerAvatarView oluşturuldu - İsim: " + username +
                ", Avatar: " + avatarId +
                ", Boyut: " + getPrefWidth() + "x" + getPrefHeight());
    }

    public void updatePlayer(Player updatedPlayer) {
        if (updatedPlayer == null) return;

        this.player = updatedPlayer;
        updateStyle();
    }

    public Player getPlayer() {
        return player;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarId() {
        return avatarId;
    }

    private void loadAvatar() {
        String defaultAvatarPath = "/images/user_avatars/avatar1.png";
        String avatarPath = (avatarId != null && !avatarId.isEmpty())
                ? "/images/user_avatars/" + avatarId + ".png"
                : defaultAvatarPath;

        try {
            // Önce avatar görüntüsünü yüklemeyi dene
            Image image = loadImageFromResource(avatarPath);

            // Yüklenemezse varsayılan avatarı kullan
            if (image == null) {
                System.err.println("Avatar yüklenemedi: " + avatarPath + ", varsayılan kullanılıyor.");
                image = loadImageFromResource(defaultAvatarPath);
            }

            // Hala null ise görünür bir hata simgesi kullan
            if (image == null) {
                System.err.println("Varsayılan avatar da yüklenemedi!");
                // 1x1 pixel kırmızı uyarı görüntüsü oluştur
                image = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
            }

            avatarImage.setImage(image);

            // Ölüm overlay görüntüsünü yükle
            Image overlayImage = loadImageFromResource("/images/dead_overlay.png");
            if (overlayImage != null) {
                deadOverlay.setImage(overlayImage);
            }

            System.out.println("Avatar başarıyla yüklendi: " + avatarPath);

        } catch (Exception e) {
            System.err.println("Avatar yüklenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Image loadImageFromResource(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                System.err.println("Kaynak bulunamadı: " + resourcePath);
                return null;
            }
            return new Image(is);

        } catch (Exception e) {
            System.err.println("Görüntü yüklenirken hata: " + resourcePath + " - " + e.getMessage());
            return null;
        }
    }

    private void updateStyle() {
        boolean isAlive = player != null && player.isAlive();

        if (!isAlive) {
            // Ölü görünümü
            getStyleClass().add("player-dead");
            nameLabel.getStyleClass().add("player-dead-label");
            avatarImage.setOpacity(0.5);

            // Overlay'i göster
            if (deadOverlay.getImage() != null) {
                if (!avatarContainer.getChildren().contains(deadOverlay)) {
                    avatarContainer.getChildren().add(deadOverlay);
                }
                deadOverlay.setVisible(true);
            }

        } else {
            // Canlı görünümü
            getStyleClass().removeAll("player-dead");
            nameLabel.getStyleClass().removeAll("player-dead-label");
            avatarImage.setOpacity(1.0);

            // Overlay'i gizle
            deadOverlay.setVisible(false);
            avatarContainer.getChildren().remove(deadOverlay);
        }
    }

    @Override
    public String toString() {
        return "PlayerAvatarView{username='" + username + "', avatarId='" + avatarId + "'}";
    }
}