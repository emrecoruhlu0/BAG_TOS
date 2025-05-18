package com.bag_tos.client.view;

import com.bag_tos.client.ClientApplication;
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

import java.util.ArrayList;
import java.util.List;

public class LobbyView extends BorderPane {
    private Text titleText;
    private Text readyCountText;
    private PlayerListView playerListView;
    private ChatPanel chatPanel;
    private Button readyButton;
    private Button startButton;

    private List<Player> players = new ArrayList<>();

    public LobbyView() {
        setPadding(new Insets(10));
        getStyleClass().add("lobby-view");

        // Başlık
        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);

        titleText = new Text("Lobi");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 20));

        readyCountText = new Text("Hazır: 0/4");
        readyCountText.setFont(Font.font("System", 14));

        topBox.getChildren().addAll(titleText, readyCountText);
        BorderPane.setAlignment(topBox, Pos.CENTER);
        BorderPane.setMargin(topBox, new Insets(10));

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
        setTop(topBox);
        setLeft(playerListView);
        setCenter(chatPanel);
        setBottom(buttonBox);
    }

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

    public void updatePlayerList(List<Player> newPlayers) {
        this.players.clear();
        this.players.addAll(newPlayers);
        playerListView.updatePlayers(players);
    }

    public void addChatMessage(String message) {
        chatPanel.addMessage(message);
    }

    public void updateReadyCount(int count) {
        readyCountText.setText("Hazır: " + count + "/4");
    }

    // Getter metodları
    public PlayerListView getPlayerListView() {
        return playerListView;
    }

    public TextField getMessageField() {
        return chatPanel.getMessageField();
    }

    public Button getSendButton() {
        return chatPanel.getSendButton();
    }

    public Button getReadyButton() {
        return readyButton;
    }

    public Button getStartButton() {
        return startButton;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}