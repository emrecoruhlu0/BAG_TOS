package com.bag_tos.client.view;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.model.Player;
import com.bag_tos.client.view.components.ActionPanel;
import com.bag_tos.client.view.components.ChatPanel;
import com.bag_tos.client.view.components.PlayerListView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Oyun ekranı görünümü
 */
public class GameView extends BorderPane {
    // Üst bilgi alanı
    private Text phaseText;
    private Text roleText;
    private Text timeText;

    // Ana bileşenler
    private PlayerListView playerListView;
    private TabPane chatTabPane;
    private ActionPanel actionPanel;

    // Sekme içerikleri
    private ChatPanel generalChatPanel;
    private ChatPanel mafiaChatPanel;
    private TextArea systemMessagesArea;

    /**
     * Oyun görünümü oluşturur
     */
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

    /**
     * Üst bilgi alanını oluşturur
     *
     * @return Üst bilgi alanı
     */
    private HBox createInfoBox() {
        HBox infoBox = new HBox(20);
        infoBox.setPadding(new Insets(10));
        infoBox.setAlignment(Pos.CENTER);

        phaseText = new Text("FAZ: LOBI");
        phaseText.setFont(Font.font("System", FontWeight.BOLD, 16));

        roleText = new Text("ROL: -");
        roleText.setFont(Font.font("System", FontWeight.BOLD, 16));

        timeText = new Text("SÜRE: -");
        timeText.setFont(Font.font("System", FontWeight.BOLD, 16));

        infoBox.getChildren().addAll(phaseText, roleText, timeText);
        return infoBox;
    }

    /**
     * Sohbet alanını oluşturur
     *
     * @return Sohbet sekmeli paneli
     */
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

        // Sistem mesajları sekmesi
        Tab systemMessagesTab = new Tab("Sistem Mesajları");
        systemMessagesTab.setClosable(false);
        systemMessagesArea = new TextArea();
        systemMessagesArea.setEditable(false);
        systemMessagesArea.setWrapText(true);
        systemMessagesTab.setContent(systemMessagesArea);

        tabPane.getTabs().addAll(generalChatTab, mafiaChatTab, systemMessagesTab);
        return tabPane;
    }

    /**
     * Faz bilgisini günceller
     *
     * @param phase Oyun fazı
     */
    public void updatePhase(GameState.Phase phase) {
        switch (phase) {
            case DAY:
                phaseText.setText("FAZ: GÜNDÜZ");
                phaseText.setFill(Color.ORANGE);
                // Ana panelde stil değişikliği yapılabilir
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
    }

    /**
     * Rol bilgisini günceller
     *
     * @param role Oyuncu rolü
     */
    public void updateRole(String role) {
        roleText.setText("ROL: " + role);
    }

    /**
     * Zamanlayıcı bilgisini günceller
     *
     * @param seconds Kalan saniye
     */
    public void updateTime(int seconds) {
        timeText.setText("SÜRE: " + seconds + " sn");
    }

    /**
     * Genel sohbete mesaj ekler
     *
     * @param message Mesaj
     */
    public void addChatMessage(String message) {
        generalChatPanel.addMessage(message);
    }

    /**
     * Mafya sohbetine mesaj ekler
     *
     * @param message Mesaj
     */
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

    /**
     * Sistem mesajları alanına mesaj ekler
     *
     * @param message Mesaj
     */
    public void addSystemMessage(String message) {
        systemMessagesArea.appendText(message + "\n");
        // Otomatik kaydırma
        systemMessagesArea.setScrollTop(Double.MAX_VALUE);
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
     * Aksiyon panelini döndürür
     *
     * @return Aksiyon paneli
     */
    public ActionPanel getActionPanel() {
        return actionPanel;
    }

    /**
     * Genel sohbet panelini döndürür
     *
     * @return Genel sohbet paneli
     */
    public ChatPanel getChatPanel() {
        return generalChatPanel;
    }

    /**
     * Mafya sohbet panelini döndürür
     *
     * @return Mafya sohbet paneli
     */
    public ChatPanel getMafiaChatPanel() {
        return mafiaChatPanel;
    }

    /**
     * Sistem mesajları alanını döndürür
     *
     * @return Sistem mesajları alanı
     */
    public TextArea getSystemMessagesArea() {
        return systemMessagesArea;
    }
}