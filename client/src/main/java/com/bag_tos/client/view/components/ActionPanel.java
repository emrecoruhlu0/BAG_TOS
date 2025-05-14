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
import java.util.function.Predicate; // Bu import eklendi
import java.util.stream.Collectors;

public class ActionPanel extends VBox {
    private Text titleText;
    private ObservableList<Player> targetPlayers;

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

    public void clearActions() {
        getChildren().clear();
        getChildren().add(titleText);
    }

    public void setTargetPlayers(List<Player> players) {
        targetPlayers.clear();
        if (players != null) {
            targetPlayers.addAll(players);
        }
    }

    public void setFilteredAlivePlayers(List<Player> players, String currentUsername, Predicate<Player> filter) {
        targetPlayers.clear();
        if (players != null && !players.isEmpty()) {
            // Debug
            System.out.println("setFilteredAlivePlayers çağrıldı, oyuncu sayısı: " + players.size());
            System.out.println("Mevcut kullanıcı: " + currentUsername);

            // Canlı ve filtreyi geçen oyuncuları ekle
            List<Player> filteredPlayers = players.stream()
                    .filter(Player::isAlive)
                    .filter(p -> !p.getUsername().equals(currentUsername)) // Kendini hedef alamama
                    .filter(filter) // Özel filtre (diğer mafya üyelerini çıkarmak için)
                    .collect(Collectors.toList());

            targetPlayers.addAll(filteredPlayers);

            // Debug
            System.out.println("Filtrelenmiş hedef listesine eklenen oyuncu sayısı: " + targetPlayers.size());
        } else {
            System.out.println("UYARI: Oyuncu listesi boş!");
        }
    }

    public ObservableList<Player> getTargetPlayersList() {
        return targetPlayers;
    }

    public void setManualTargetList(List<Player> players) {
        targetPlayers.clear();
        if (players != null) {
            targetPlayers.addAll(players);
            System.out.println("Manuel hedef listesi ayarlandı, oyuncu sayısı: " + targetPlayers.size());
        }
    }

    public void setCustomTargetList(List<Player> players) {
        targetPlayers.clear();
        if (players != null && !players.isEmpty()) {
            targetPlayers.addAll(players);
            System.out.println("Özel hedef listesi ayarlandı: " + targetPlayers.size() + " oyuncu");
        } else {
            System.out.println("UYARI: Özel hedef listesi boş!");
        }
    }

    // Oyuncular listesini ayarla
    public void setAlivePlayers(List<Player> players) {
        targetPlayers.clear();
        if (players != null && !players.isEmpty()) {
            // Debug
            System.out.println("setAlivePlayers(TÜM OYUNCULAR) çağrıldı, oyuncu sayısı: " + players.size());

            // Canlı oyuncuları ekle
            List<Player> alivePlayers = players.stream()
                    .filter(Player::isAlive)
                    .collect(Collectors.toList());

            targetPlayers.addAll(alivePlayers);

            // Debug
            System.out.println("Hedef listesine eklenen oyuncu sayısı: " + targetPlayers.size());
        } else {
            System.out.println("UYARI: Oyuncu listesi boş!");
        }
    }

    public void setAlivePlayers(List<Player> players, String currentUsername) {
        targetPlayers.clear();
        if (players != null && !players.isEmpty()) {
            // Debug
            System.out.println("setAlivePlayers(KENDİSİ HARİÇ) çağrıldı, oyuncu sayısı: " + players.size());
            System.out.println("Hariç tutulan oyuncu: " + currentUsername);

            // Canlı ve kendisi olmayan oyuncuları ekle
            List<Player> filteredPlayers = players.stream()
                    .filter(Player::isAlive)
                    .filter(p -> !p.getUsername().equals(currentUsername))
                    .collect(Collectors.toList());

            targetPlayers.addAll(filteredPlayers);

            // Debug
            System.out.println("Hedef listesine eklenen oyuncu sayısı: " + targetPlayers.size());
        } else {
            System.out.println("UYARI: Oyuncu listesi boş!");
        }
    }

    public void setAlivePlayers(List<Player> players, String currentUsername, String currentRole) {
        targetPlayers.clear();

        if (players != null && !players.isEmpty()) {
            // Debug
            System.out.println("Hedefler ekleniyor - Rol: " + currentRole + ", Kullanıcı: " + currentUsername);

            // Tüm canlı oyuncuları filtrele
            List<Player> validTargets = players.stream()
                    .filter(Player::isAlive)
                    .filter(player -> {
                        // Eğer oyuncu kendisi ise, sadece doktor için kendini hedef göster
                        if (player.getUsername().equals(currentUsername)) {
                            return "Doktor".equals(currentRole);
                        }
                        return true; // Diğer oyuncular her zaman gösterilir
                    })
                    .collect(Collectors.toList());

            targetPlayers.addAll(validTargets);

            // Debug
            System.out.println("Eklenen hedef sayısı: " + targetPlayers.size());
        } else {
            System.out.println("UYARI: Oyuncu listesi boş!");
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
            try {
                System.out.println("İnfaz butonu tıklandı");
                // Dummy player kullanmak yerine null geç, handler içinde değeri bağla
                handler.onAction(null);
            } catch (Exception ex) {
                System.err.println("İnfaz butonu işlenirken hata: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        executeActionBox.getChildren().addAll(actionLabel, executeButton);
        getChildren().add(executeActionBox);

        System.out.println("İnfaz butonu eklendi");
    }

    public void addKillAction(ActionHandler handler) {
        HBox killActionBox = new HBox(10);
        killActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("Öldür:");

        // ÖNEMLİ: ComboBox oluşturmadan önce mafya kontrolü yap
        // Mafya üyelerini hedef listesinden çıkar
        ObservableList<Player> filteredTargets = FXCollections.observableArrayList();
        for (Player p : targetPlayers) {
            String role = p.getRole();
            boolean isMafia = role != null && role.equals("Mafya");

            if (!isMafia) {
                filteredTargets.add(p);
            }
        }

        ComboBox<Player> targetCombo = new ComboBox<>(filteredTargets); // Filtrelenmiş listeyi kullan
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

    public void addHealAction(ActionHandler handler) {
        HBox healActionBox = new HBox(10);
        healActionBox.setAlignment(Pos.CENTER);

        Label actionLabel = new Label("İyileştir:");

        // Hedef listesinin boyutunu log'la
        System.out.println("addHealAction içinde tüm hedef oyuncular:");
        for (Player p : targetPlayers) {
            System.out.println(" - " + p.getUsername());
        }

        // Yeni bir observable liste oluştur ve mevcut hedefleri kopyala
        ObservableList<Player> healTargets = FXCollections.observableArrayList(targetPlayers);

        // ComboBox oluştur
        ComboBox<Player> targetCombo = new ComboBox<>(healTargets);
        targetCombo.setPromptText("Hedef seçin");
        targetCombo.setCellFactory(param -> new PlayerListCell());
        targetCombo.setButtonCell(new PlayerListCell());


        // Hedef listesini tekrar log'la
        System.out.println("ComboBox oluşturulduktan sonra hedef sayısı: " + targetPlayers.size());

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

    public int getTargetCount() {
        return targetPlayers.size();
    }

    public interface ActionHandler {
        void onAction(Player target);
    }

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