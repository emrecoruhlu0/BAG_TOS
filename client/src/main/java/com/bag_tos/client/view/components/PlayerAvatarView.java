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
    private ImageView deadOverlay; // Ölüm overlay'i için
    private StackPane avatarContainer;
    private Label nameLabel;
    private Player player;
    private final String username; // Değişmez oyuncu adı
    private final String avatarId; // Değişmez avatar ID


// PlayerAvatarView sınıfında yapılması gereken değişiklik

    public PlayerAvatarView(Player player) {
        // Önemli: İsim ve avatar ID'yi yapıcıda sabit değişken olarak sakla
        this.username = player.getUsername();
        this.avatarId = player.getAvatarId();

        // Eksik olan atama - bu satır eklenmeli
        this.player = player;

        this.avatarContainer = avatarContainer; // Bu satırı ekle


        setAlignment(Pos.CENTER);
        setSpacing(5);
        setPrefSize(100, 120);
        getStyleClass().add("player-avatar-view");

        // Avatar görüntüsü
        avatarImage = new ImageView();
        avatarImage.setFitWidth(80);
        avatarImage.setFitHeight(80);
        avatarImage.setPreserveRatio(true);

        // Avatar arkaplanı
        StackPane avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("avatar-container");
        avatarContainer.getChildren().add(avatarImage);

        // İsim etiketi - sabit oyuncu ismi kullan
        nameLabel = new Label(username);
        nameLabel.getStyleClass().add("player-name-label");

        getChildren().addAll(avatarContainer, nameLabel);

        // Görüntüyü yükle
        loadAvatar();

        // Debug
        System.out.println("PlayerAvatarView oluşturuldu - İsim: " + username + ", Avatar: " + avatarId);
    }

    public void updatePlayer(Player updatedPlayer) {
        // Yeni oyuncu referansını saklayın
        this.player = updatedPlayer;

        // Görünümü güncelleyin
        nameLabel.setText(updatedPlayer.getUsername());
        updateStyle();
        loadAvatar();
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
        // Saklanan sabit avatar ID'yi kullan, player referansına güvenme
        String avatarPath = "/images/user_avatars/avatar1.png"; // Varsayılan

        if (avatarId != null && !avatarId.isEmpty()) {
            avatarPath = "/images/user_avatars/" + avatarId + ".png";
        }

        try {
            InputStream is = getClass().getResourceAsStream(avatarPath);
            if (is == null) {
                System.err.println("Avatar dosyası bulunamadı: " + avatarPath);
                // Varsayılan avatar yolunu dene
                avatarPath = "/images/user_avatars/avatar1.png";
                is = getClass().getResourceAsStream(avatarPath);

                if (is == null) {
                    System.err.println("Varsayılan avatar da bulunamadı!");
                    return;
                }
            }

            Image image = new Image(is);
            avatarImage.setImage(image);
            System.out.println("Avatar başarıyla yüklendi: " + avatarPath);
        } catch (Exception e) {
            System.err.println("Avatar yüklenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Ölüm durumu gösterimini iyileştir
    private void updateStyle() {
        if (!player.isAlive()) {
            getStyleClass().add("player-dead");

            // Ölü görünümünü uygula
            applyDeadOverlay();

            // İsim etiketini de güncelle
            nameLabel.getStyleClass().add("player-dead-label");

            System.out.println("Ölü görünümü uygulandı: " + player.getUsername());
        } else {
            getStyleClass().remove("player-dead");

            // Ölü görünümünü kaldır
            removeDeadOverlay();

            // İsim etiketinden stili kaldır
            nameLabel.getStyleClass().remove("player-dead-label");
        }
    }

    private void applyDeadOverlay() {
        try {
            // Şimdilik resim yüklemesini atla, direkt alternatif görünüm kullan
            System.out.println("Ölü görünümü uygulanıyor (alternatif): " + player.getUsername());
            applyAlternativeDeadEffect();

        /* Dead overlay resmi için (şimdilik kapalı):
        if (deadOverlay == null) {
            InputStream overlayStream = getClass().getResourceAsStream("/images/dead_overlay.png");
            if (overlayStream != null) {
                deadOverlay = new ImageView(new Image(overlayStream));
                deadOverlay.setFitWidth(80);
                deadOverlay.setFitHeight(80);
                deadOverlay.setPreserveRatio(true);
                deadOverlay.setMouseTransparent(true);

                if (!avatarContainer.getChildren().contains(deadOverlay)) {
                    avatarContainer.getChildren().add(deadOverlay);
                }
            }
        }
        */

        } catch (Exception e) {
            System.err.println("Ölü overlay'i uygulanırken hata: " + e.getMessage());
            applyAlternativeDeadEffect();
        }
    }

    private void applyAlternativeDeadEffect() {
        // Dead overlay yoksa güçlü görsel efekt
        avatarImage.setOpacity(0.3);

        // Kırmızı gölge efekti
        avatarImage.setStyle("-fx-effect: dropshadow(gaussian, rgba(139, 0, 0, 0.9), 20, 0.8, 0, 0);");

        // Avatar container'a çapraz çizgi ve güçlü kenarlık efekti
        avatarContainer.setStyle(
                "-fx-border-color: #8B0000;" +
                        "-fx-border-width: 4px;" +
                        "-fx-border-radius: 40px;" +
                        "-fx-background-color: rgba(139, 0, 0, 0.1);" +
                        "-fx-effect: dropshadow(gaussian, rgba(139, 0, 0, 1.0), 15, 0.8, 0, 0);"
        );

        System.out.println("Alternatif ölü efekti uygulandı: " + player.getUsername());
    }


    private void removeDeadOverlay() {
        // Eğer overlay varsa ve eklenmiş ise kaldır
        if (deadOverlay != null && avatarContainer.getChildren().contains(deadOverlay)) {
            avatarContainer.getChildren().remove(deadOverlay);
        }

        // Avatarı normal opaklığa getir
        avatarImage.setOpacity(1.0);
        avatarImage.setStyle(""); // Efektleri temizle

        // Container stilini temizle
        avatarContainer.setStyle("");
    }


    public void updateStatus(boolean alive) {
        if (!alive) {
            getStyleClass().add("player-dead");
            avatarImage.setOpacity(0.5);
        } else {
            getStyleClass().remove("player-dead");
            avatarImage.setOpacity(1.0);
        }
    }

    private void setupMouseEvents() {
        // Fare tıklaması
        setOnMouseClicked(e -> {
            System.out.println("Oyuncu seçildi: " + player.getUsername());
            // Burada oyuncu seçimi olayını tetikleyebilirsiniz
        });

        // Fare üzerine gelince bilgi gösterme
        Tooltip tooltip = new Tooltip();
        tooltip.setText("Oyuncu: " + player.getUsername() + "\nDurum: " + (player.isAlive() ? "Hayatta" : "Ölü"));
        Tooltip.install(this, tooltip);
    }

    public void update() {
        nameLabel.setText(player.getUsername());
        updateStyle();
        loadAvatar();
    }
}