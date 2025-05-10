package com.bag_tos.client.view.components;

import com.bag_tos.client.model.Player;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Oyuncu listesini gösteren yeniden kullanılabilir bileşen
 */
public class PlayerListView extends VBox {
    private ListView<Player> playerListView;
    private Label titleLabel;
    private ObservableList<Player> players;
    private PlayerSelectedHandler playerSelectedHandler;

    /**
     * Varsayılan oyuncu listesi bileşeni oluşturur
     */
    public PlayerListView() {
        this("Oyuncular");
    }

    /**
     * Özel başlık ile oyuncu listesi bileşeni oluşturur
     *
     * @param title Başlık metni
     */
    public PlayerListView(String title) {
        setPadding(new Insets(10));
        setSpacing(10);
        getStyleClass().add("player-list");

        // Başlık
        titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Oyuncu listesi
        players = FXCollections.observableArrayList();
        playerListView = new ListView<>(players);
        playerListView.setPrefWidth(200);

        // Özel hücre fabrikası - oyuncunun durumuna göre stil
        playerListView.setCellFactory(param -> new ListCell<Player>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);

                if (empty || player == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Oyuncu adı ve durumunu göster
                    setText(player.getUsername());

                    // Oyuncunun hayatta olma durumuna göre stil
                    if (player.isAlive()) {
                        getStyleClass().add("character-alive");
                        getStyleClass().remove("character-dead");
                    } else {
                        getStyleClass().add("character-dead");
                        getStyleClass().remove("character-alive");
                    }
                }
            }
        });

        // Oyuncu seçildiğinde işleyiciyi çağır
        playerListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && playerSelectedHandler != null) {
                        playerSelectedHandler.onSelect(newValue);
                    }
                }
        );

        getChildren().addAll(titleLabel, playerListView);
    }

    /**
     * Oyuncu listesini günceller
     *
     * @param playerList Güncellenecek oyuncu listesi
     */
    public void updatePlayers(List<Player> playerList) {
        if (playerList == null) return;

        players.clear();
        players.addAll(playerList);
    }

    /**
     * Oyuncu seçildiğinde çağrılacak işleyiciyi ayarlar
     *
     * @param handler Oyuncu seçim işleyicisi
     */
    public void setOnPlayerSelected(PlayerSelectedHandler handler) {
        this.playerSelectedHandler = handler;
    }

    /**
     * Oyuncu seçimi için işleyici arayüzü
     */
    public interface PlayerSelectedHandler {
        void onSelect(Player player);
    }

    /**
     * Başlık metnini ayarlar
     *
     * @param title Yeni başlık metni
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * ListView kontrolünü döndürür
     *
     * @return Oyuncu listesi kontrolü
     */
    public ListView<Player> getListView() {
        return playerListView;
    }

    /**
     * Oyuncular koleksiyonunu döndürür
     *
     * @return Oyuncular ObservableList koleksiyonu
     */
    public ObservableList<Player> getPlayers() {
        return players;
    }
}