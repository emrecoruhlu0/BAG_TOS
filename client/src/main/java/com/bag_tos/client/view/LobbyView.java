package com.bag_tos.client.view;

import com.bag_tos.client.model.Player;
import com.bag_tos.client.view.components.ChatPanel;
import com.bag_tos.client.view.components.PlayerListView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Lobi ekranı görünümü
 */
public class LobbyView extends BorderPane {
    private Text titleText;
    private PlayerListView playerListView;
    private ChatPanel chatPanel;
    private Button readyButton;
    private Button startButton;

    /**
     * Lobi görünümü oluşturur
     */
    public LobbyView() {
        setPadding(new Insets(10));
        getStyleClass().add("lobby-view");

        // Başlık
        titleText = new Text("Lobi");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 20));
        BorderPane.setAlignment(titleText, Pos.CENTER);
        BorderPane.setMargin(titleText, new Insets(10));

        // Oyuncu listesi
        playerListView = new PlayerListView("Oyuncular");
        playerListView.setTitle("Oyuncular");
        BorderPane.setMargin(playerListView, new Insets(10));

        // Sohbet alanı
        chatPanel = new ChatPanel("Mesajınızı yazın...");
        BorderPane.setMargin(chatPanel, new Insets(10));

        // Butonlar
        HBox buttonBox = createButtonBox();

        // Layout yerleşimi
        setTop(titleText);
        setLeft(playerListView);
        setCenter(chatPanel);
        setBottom(buttonBox);
    }

    /**
     * Butonları içeren HBox oluşturur
     *
     * @return Butonlar HBox'ı
     */
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(20);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER);

        readyButton = new Button("Hazır");
        readyButton.setPrefWidth(150);
        readyButton.getStyleClass().add("primary-button");

        startButton = new Button("Başlat");
        startButton.setPrefWidth(150);
        startButton.getStyleClass().add("secondary-button");

        buttonBox.getChildren().addAll(readyButton, startButton);
        return buttonBox;
    }

    /**
     * Oyuncu listesini günceller
     *
     * @param players Güncellenecek oyuncu listesi
     */
    public void updatePlayerList(List<Player> players) {
        playerListView.updatePlayers(players);
    }

    /**
     * Sohbet alanına mesaj ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addChatMessage(String message) {
        chatPanel.addMessage(message);
    }

    // Getter metodları

    /**
     * Oyuncu listesi görünümünü döndürür
     *
     * @return Oyuncu listesi görünümü
     */
    public PlayerListView getPlayerListView() {
        return playerListView;
    }

    /**
     * Mesaj alanını döndürür
     *
     * @return Mesaj alanı
     */
    public TextField getMessageField() {
        return chatPanel.getMessageField();
    }

    /**
     * Gönder butonunu döndürür
     *
     * @return Gönder butonu
     */
    public Button getSendButton() {
        return chatPanel.getSendButton();
    }

    /**
     * Hazır butonunu döndürür
     *
     * @return Hazır butonu
     */
    public Button getReadyButton() {
        return readyButton;
    }

    /**
     * Başlat butonunu döndürür
     *
     * @return Başlat butonu
     */
    public Button getStartButton() {
        return startButton;
    }

    /**
     * Sohbet panelini döndürür
     *
     * @return Sohbet paneli
     */
    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}