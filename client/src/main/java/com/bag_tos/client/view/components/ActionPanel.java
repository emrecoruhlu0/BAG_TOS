package com.bag_tos.client.view.components;

import com.bag_tos.client.model.Player;
import com.bag_tos.common.config.GameConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Oyun içi aksiyonları gösteren panel
 */
public class ActionPanel extends VBox {
    private Text titleText;
    private ObservableList<Player> targetPlayers;

    /**
     * Aksiyon paneli oluşturur
     */
    public ActionPanel() {
        setPadding(new Insets(15));
        setSpacing(10);
        setAlignment(Pos.CENTER);
        getStyleClass().add("action-panel");

        titleText = new Text("Aksiyonlar");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 16));

        targetPlayers = FXCollections.observableArrayList();

        getChildren().add(titleText);
    }

    /**
     * Tüm aksiyonları temizler
     */
    public void clearActions() {
        getChildren().clear();
        getChildren().add(titleText);
    }

    /**
     * Hedef oyuncu listesini ayarlar
     *
     * @param players Hedef oyuncu listesi
     */
    public void setTargetPlayers(List<Player> players) {
        targetPlayers.clear();
        if (players != null) {
            targetPlayers.addAll(players);
        }
    }

    /**
     * Sadece yaşayan oyuncuları hedef listesine ekler
     *
     * @param players Tüm oyuncular
     */
    public void setAlivePlayers(List<Player> players) {
        targetPlayers.clear();

        if (players != null && !players.isEmpty()) {
            // Oyuncu listesini konsola yazdır (hata ayıklama)
            System.out.println("Hedefler ekleniyor, oyuncu sayısı: " + players.size());
            for (Player p : players) {
                System.out.println("Oyuncu: " + p.getUsername() + ", Hayatta: " + p.isAlive());
            }

            // Canlı oyuncuları ekle
            List<Player> alivePlayers = players.stream()
                    .filter(Player::isAlive)
                    .collect(Collectors.toList());

            targetPlayers.addAll(alivePlayers);

            // Hedef listesini kontrol et
            System.out.println("Eklenen hedef sayısı: " + targetPlayers.size());
        } else {
            System.out.println("UYARI: Oyuncu listesi boş!");
        }
    }

    /**
     * Sadece yaşayan oyuncuları hedef listesine ekler, kendisini filtreler
     *
     * @param players Tüm oyuncular
     * @param currentUsername Mevcut oyuncunun kullanıcı adı
     */
    public void setAlivePlayers(List<Player> players, String currentUsername) {
        targetPlayers.clear();
        if (players != null) {
            List<Player> filteredPlayers = players.stream()
                    .filter(Player::isAlive)
                    .filter(p -> !p.getUsername().equals(currentUsername))
                    .collect(Collectors.toList());

            targetPlayers.addAll(filteredPlayers);
        }
    }

    public void addJailAction(ActionHandler handler) {
        HBox jailActionBox = new HBox(10);
        jailActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Hapse At:");

        ComboBox<Player> targetCombo = new ComboBox<>(targetPlayers);
        targetCombo.setPromptText("Hedef seçin");
        targetCombo.setCellFactory(param -> new PlayerListCell());
        targetCombo.setButtonCell(new PlayerListCell());

        Button jailButton = new Button("Hapse At");
        jailButton.getStyleClass().add("warning-button");
        jailButton.setOnAction(e -> {
            Player selectedTarget = targetCombo.getValue();
            if (selectedTarget != null) {
                handler.onAction(selectedTarget);
                targetCombo.setValue(null);
            }
        });

        jailActionBox.getChildren().addAll(actionLabel, targetCombo, jailButton);
        getChildren().add(jailActionBox);
    }

    public void addExecuteAction(ActionHandler handler) {
        HBox executeActionBox = new HBox(10);
        executeActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("İnfaz Et:");

        Button executeButton = new Button("İnfaz Et");
        executeButton.getStyleClass().add("danger-button");
        executeButton.setOnAction(e -> {
            // Hapsedilen kişi zaten belirli olduğu için doğrudan işle
            Player dummyPlayer = new Player("Hapsedilen");
            handler.onAction(dummyPlayer);
        });

        executeActionBox.getChildren().addAll(actionLabel, executeButton);
        getChildren().add(executeActionBox);
    }

    public void addKillAction(ActionHandler handler) {
        HBox killActionBox = new HBox(10);
        killActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Öldür:");

        ComboBox<Player> targetCombo = new ComboBox<>(targetPlayers);
        targetCombo.setPromptText("Hedef seçin");
        targetCombo.setCellFactory(param -> new PlayerListCell());
        targetCombo.setButtonCell(new PlayerListCell());

        Button killButton = new Button("Öldür");
        killButton.getStyleClass().add("danger-button");
        killButton.setOnAction(e -> {
            Player selectedTarget = targetCombo.getValue();
            if (selectedTarget != null) {
                handler.onAction(selectedTarget);
                // Aksiyon sonrası UI state'ini güncelle
                targetCombo.setValue(null);
            }
        });

        killActionBox.getChildren().addAll(actionLabel, targetCombo, killButton);
        getChildren().add(killActionBox);
    }

    /**
     * Doktor için iyileştirme aksiyonu ekler
     *
     * @param handler Aksiyon işleyicisi
     */
    public void addHealAction(ActionHandler handler) {
        HBox healActionBox = new HBox(10);
        healActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("İyileştir:");

        ComboBox<Player> targetCombo = new ComboBox<>(targetPlayers);
        targetCombo.setPromptText("Hedef seçin");
        targetCombo.setCellFactory(param -> new PlayerListCell());
        targetCombo.setButtonCell(new PlayerListCell());

        Button healButton = new Button("İyileştir");
        healButton.getStyleClass().add("primary-button");
        healButton.setOnAction(e -> {
            Player selectedTarget = targetCombo.getValue();
            if (selectedTarget != null) {
                handler.onAction(selectedTarget);
                // Aksiyon sonrası UI state'ini güncelle
                targetCombo.setValue(null);
            }
        });

        healActionBox.getChildren().addAll(actionLabel, targetCombo, healButton);
        getChildren().add(healActionBox);
    }

    /**
     * Oylama aksiyonu ekler
     *
     * @param handler Aksiyon işleyicisi
     */
    public void addVoteAction(ActionHandler handler) {
        HBox voteActionBox = new HBox(10);
        voteActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Oyla:");

        ComboBox<Player> targetCombo = new ComboBox<>(targetPlayers);
        targetCombo.setPromptText("Hedef seçin");
        targetCombo.setCellFactory(param -> new PlayerListCell());
        targetCombo.setButtonCell(new PlayerListCell());

        Button voteButton = new Button("Oyla");
        voteButton.getStyleClass().add("secondary-button");
        voteButton.setOnAction(e -> {
            Player selectedTarget = targetCombo.getValue();
            if (selectedTarget != null) {
                handler.onAction(selectedTarget);
                // Aksiyon sonrası UI state'ini güncelle
                targetCombo.setValue(null);
            }
        });

        voteActionBox.getChildren().addAll(actionLabel, targetCombo, voteButton);
        getChildren().add(voteActionBox);
    }

    /**
     * Şerif için araştırma aksiyonu ekler
     *
     * @param handler Aksiyon işleyicisi
     */
    public void addInvestigateAction(ActionHandler handler) {
        HBox investigateActionBox = new HBox(10);
        investigateActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Araştır:");

        ComboBox<Player> targetCombo = new ComboBox<>(targetPlayers);
        targetCombo.setPromptText("Hedef seçin");
        targetCombo.setCellFactory(param -> new PlayerListCell());
        targetCombo.setButtonCell(new PlayerListCell());

        Button investigateButton = new Button("Araştır");
        investigateButton.getStyleClass().add("primary-button");
        investigateButton.setOnAction(e -> {
            Player selectedTarget = targetCombo.getValue();
            if (selectedTarget != null) {
                handler.onAction(selectedTarget);
                // Aksiyon sonrası UI state'ini güncelle
                targetCombo.setValue(null);
            }
        });

        investigateActionBox.getChildren().addAll(actionLabel, targetCombo, investigateButton);
        getChildren().add(investigateActionBox);
    }

    /**
     * Hedef listesindeki oyuncu sayısını döndürür
     */
    public int getTargetCount() {
        return targetPlayers.size();
    }

    /**
     * Aksiyon için işleyici arayüzü
     */
    public interface ActionHandler {
        void onAction(Player target);
    }

    /**
     * Oyuncuların gösterimi için özel liste hücresi
     */
    private static class PlayerListCell extends javafx.scene.control.ListCell<Player> {
        @Override
        protected void updateItem(Player player, boolean empty) {
            super.updateItem(player, empty);

            if (empty || player == null) {
                setText(null);
            } else {
                setText(player.getUsername());
            }
        }
    }
}