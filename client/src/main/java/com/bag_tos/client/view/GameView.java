package com.bag_tos.client.view;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.view.components.ActionPanel;
import com.bag_tos.client.view.components.ChatPanel;
import com.bag_tos.client.view.components.PlayerListView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;


public class GameView extends BorderPane {
    // Üst bilgi alanı
    private Text phaseText;
    private Text roleText;
    private Text timeText;
    private Text usernameText;

    // Ana bileşenler
    private PlayerListView playerListView;
    private TabPane chatTabPane;
    private ActionPanel actionPanel;

    // Sekme içerikleri
    private ChatPanel generalChatPanel;
    private ChatPanel mafiaChatPanel;
    private TextArea systemMessagesArea;

    private ChatPanel jailChatPanel;
    private Tab jailChatTab;

    public GameView() {
        setPadding(new Insets(10));
        getStyleClass().add("game-view");

        // Üst bilgiler
        HBox infoBox = createInfoBox();

        // Oyuncu listesi
        playerListView = new PlayerListView("Oyuncular");
        playerListView.setTitle("Oyuncular");

        // Sohbet alanı
        chatTabPane = createChatArea();

        // Aksiyon alanı
        actionPanel = new ActionPanel();

        // Layout yerleşimi
        setTop(infoBox);
        setLeft(playerListView);
        setCenter(chatTabPane);
        setBottom(actionPanel);
    }


    private HBox createInfoBox() {
        HBox infoBox = new HBox(20);
        infoBox.setPadding(new Insets(10));
        infoBox.setAlignment(Pos.CENTER);

        // Kullanıcı adı için alanı sol tarafta göster
        usernameText = new Text("OYUNCU: -");
        usernameText.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Mevcut bilgiler
        phaseText = new Text("FAZ: LOBI");
        phaseText.setFont(Font.font("System", FontWeight.BOLD, 16));

        roleText = new Text("ROL: -");
        roleText.setFont(Font.font("System", FontWeight.BOLD, 16));

        timeText = new Text("SÜRE: -");
        timeText.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Oyuncu adını sola, diğer bilgileri ortaya hizala
        HBox centerBox = new HBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(phaseText, roleText, timeText);

        // İnfoBox içinde yerleşim
        infoBox.getChildren().add(usernameText);

        // Ortadaki kutuyu genişletmek için sahne genişliğine bağla
        HBox.setHgrow(centerBox, Priority.ALWAYS);
        infoBox.getChildren().add(centerBox);

        return infoBox;
    }


    private TabPane createChatArea() {
        TabPane tabPane = new TabPane();

        // Genel sohbet sekmesi
        Tab generalChatTab = new Tab("Genel Sohbet");
        generalChatTab.setClosable(false);
        generalChatPanel = new ChatPanel("Mesajınızı yazın...");
        generalChatTab.setContent(generalChatPanel);

        // Mafya sohbeti sekmesi
        Tab mafiaChatTab = new Tab("Mafya Sohbeti");
        mafiaChatTab.setClosable(false);
        mafiaChatPanel = new ChatPanel("Mafya mesajınızı yazın...");
        mafiaChatTab.setContent(mafiaChatPanel);

        // Hapishane sohbeti sekmesi
        jailChatTab = new Tab("Hapishane");
        jailChatTab.setClosable(false);
        jailChatPanel = new ChatPanel("Mesajınızı yazın...");
        jailChatTab.setContent(jailChatPanel);

        // Sistem mesajları sekmesi
        Tab systemMessagesTab = new Tab("Sistem Mesajları");
        systemMessagesTab.setClosable(false);
        systemMessagesArea = new TextArea();
        systemMessagesArea.setEditable(false);
        systemMessagesArea.setWrapText(true);
        systemMessagesTab.setContent(systemMessagesArea);

        // Başlangıçta hapishane sekmesini gizle
        jailChatTab.setDisable(true);

        tabPane.getTabs().addAll(generalChatTab, mafiaChatTab, systemMessagesTab, jailChatTab);
        return tabPane;
    }

    // Hapishane sohbetini göstermek/gizlemek için yeni metotlar
    public void showJailChat() {
        Platform.runLater(() -> {
            try {
                jailChatTab.setDisable(false);
                // Tab'ı doğrudan seç
                chatTabPane.getSelectionModel().select(jailChatTab);
                System.out.println("Hapishane sohbet tab'ı aktifleştirildi ve seçildi");
            } catch (Exception e) {
                System.err.println("Hapishane tab'ı gösterilirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void hideJailChat() {
            jailChatTab.setDisable(true);
            jailChatPanel.clearMessages();
    }

    public void addJailMessage(String message) {
            jailChatPanel.addMessage(message);
    }

    // Getter metodu
    public ChatPanel getJailChatPanel() {
        return jailChatPanel;
    }

    public void updatePhase(GameState.Phase phase) {
        System.out.println("View Faz Güncellemesi: " + phase); // Debug log

        Platform.runLater(() -> {
            switch (phase) {
                case DAY:
                    phaseText.setText("FAZ: GÜNDÜZ");
                    phaseText.setFill(Color.ORANGE);
                    // Ana panelde stil değişikliği
                    this.getStyleClass().remove("night-phase");
                    this.getStyleClass().add("day-phase");
                    break;
                case NIGHT:
                    phaseText.setText("FAZ: GECE");
                    phaseText.setFill(Color.BLUE);
                    this.getStyleClass().remove("day-phase");
                    this.getStyleClass().add("night-phase");
                    break;
                default:
                    phaseText.setText("FAZ: LOBI");
                    phaseText.setFill(Color.BLACK);
                    this.getStyleClass().remove("day-phase");
                    this.getStyleClass().remove("night-phase");
            }
        });
    }

    public void updateUsername(String username) {
        usernameText.setText("OYUNCU: " + username);
    }

    public void updateRole(String role) {
        roleText.setText("ROL: " + role);
    }


    public void updateTime(int seconds) {
        timeText.setText("SÜRE: " + seconds + " sn");
    }


    public void addChatMessage(String message) {
        generalChatPanel.addMessage(message);
    }


    public void addMafiaMessage(String message) {
        mafiaChatPanel.addMessage(message);

        // Mafya olmayan oyuncular için mafya sekmesini gizle
        if (generalChatPanel.getParent().getParent() instanceof TabPane) {
            TabPane tabPane = (TabPane) generalChatPanel.getParent().getParent();

            // Eğer mafya sekmesi yoksa, mesaj geldiğinde görünür yap
            Tab mafiaTab = tabPane.getTabs().stream()
                    .filter(tab -> tab.getText().equals("Mafya Sohbeti"))
                    .findFirst()
                    .orElse(null);

            if (mafiaTab != null && !mafiaTab.isDisable()) {
                tabPane.getSelectionModel().select(mafiaTab);
            }
        }
    }


    public void addSystemMessage(String message) {
        systemMessagesArea.appendText(message + "\n");
        // Otomatik kaydırma
        systemMessagesArea.setScrollTop(Double.MAX_VALUE);
    }

    // Getter metodları

    public PlayerListView getPlayerListView() {
        return playerListView;
    }


    public ActionPanel getActionPanel() {
        return actionPanel;
    }


    public ChatPanel getChatPanel() {
        return generalChatPanel;
    }


    public ChatPanel getMafiaChatPanel() {
        return mafiaChatPanel;
    }


    public TextArea getSystemMessagesArea() {
        return systemMessagesArea;
    }
}