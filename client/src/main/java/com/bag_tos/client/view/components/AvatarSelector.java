package com.bag_tos.client.view.components;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AvatarSelector extends HBox {
    private List<ToggleButton> avatarButtons = new ArrayList<>();
    private ToggleGroup toggleGroup = new ToggleGroup();
    private String selectedAvatarId = "avatar1"; // Varsayılan

    public AvatarSelector() {
        setSpacing(10);
        setAlignment(javafx.geometry.Pos.CENTER);

        // Avatar seçeneklerini oluştur
        createAvatarOptions();

        // İlk avatarı seçili yap
        if (!avatarButtons.isEmpty()) {
            avatarButtons.get(0).setSelected(true);
        }
    }

    private void createAvatarOptions() {
        // Avatar görüntüleri için dosya yolları
        String[] avatarIds = {"avatar1", "avatar2", "avatar3", "avatar4", "avatar5", "avatar6"};

        for (String avatarId : avatarIds) {
            try {
                // Avatar görseli yükle
                InputStream inputStream = getClass().getResourceAsStream("/images/user_avatars/" + avatarId + ".png");
                if (inputStream == null) {
                    System.err.println("Avatar bulunamadı: " + avatarId);
                    continue;
                }

                Image avatarImage = new Image(inputStream);
                ImageView imageView = new ImageView(avatarImage);
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);

                // Avatar seçim butonu oluştur
                ToggleButton avatarButton = new ToggleButton();
                avatarButton.setToggleGroup(toggleGroup);
                avatarButton.setGraphic(imageView);
                avatarButton.setUserData(avatarId);
                avatarButton.getStyleClass().add("avatar-toggle-button");

                // Tıklama olayı
                avatarButton.setOnAction(e -> {
                    selectedAvatarId = (String) avatarButton.getUserData();
                });

                avatarButtons.add(avatarButton);
                getChildren().add(avatarButton);

            } catch (Exception e) {
                System.err.println("Avatar yüklenirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public String getSelectedAvatarId() {
        System.out.println("AvatarSelector.getSelectedAvatarId() = " + selectedAvatarId);
        return selectedAvatarId;
    }
}