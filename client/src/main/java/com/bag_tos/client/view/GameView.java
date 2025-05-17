package com.bag_tos.client.view;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.view.components.ActionPanel;
import com.bag_tos.client.view.components.ChatPanel;
import com.bag_tos.client.view.components.PlayerCircleView;
import com.bag_tos.client.view.components.PlayerListView;
// YENİ: VoiceControlPanel import'u ekleyin
import com.bag_tos.client.view.components.VoiceControlPanel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


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

    private ImageView roleAvatarView;
    private Label roleNameLabel;
    private PlayerCircleView playerCircleView; // Çember görünümü

    private VoiceControlPanel voiceControlPanel;

    public GameView() {
        setPadding(new Insets(10));
        getStyleClass().add("game-view");

        // Rol avatarı için panel - sol üst köşede
        VBox roleInfoBox = createRoleInfoBox();

        // Üst bilgiler
        HBox infoBox = createInfoBox();

        // PlayerCircleView - test etmek için sınır ekleyelim
        playerCircleView = new PlayerCircleView();
        playerCircleView.setStyle("-fx-border-color: red; -fx-border-width: 2px;"); // Test için görünür kenarlık
        playerCircleView.setPrefSize(500, 500);
        playerCircleView.setMinSize(300, 300);
        playerCircleView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // PlayerListView'u oluşturun (artık görünmeyecek olsa bile)
        playerListView = new PlayerListView(); // Bu satırın eklenmesi önemli

        // Oyuncu çemberi - orta kısımda
        //playerCircleView = new PlayerCircleView();

        // Sohbet alanı - sağ tarafta
        chatTabPane = createChatArea();

        // Aksiyon alanı - alt kısımda
        actionPanel = new ActionPanel();

        voiceControlPanel = new VoiceControlPanel();

        // Layout yerleşimi
        BorderPane topSection = new BorderPane();
        topSection.setLeft(roleInfoBox);
        topSection.setCenter(infoBox);

        BorderPane.setMargin(playerCircleView, new Insets(10));

        topSection.setRight(voiceControlPanel);

        setTop(topSection);
        setCenter(playerCircleView); // Artık playerListView değil, playerCircleView kullanıyoruz
        setRight(chatTabPane);
        setBottom(actionPanel);

        System.out.println("GameView oluşturuldu. PlayerCircleView merkezde.");

        // PlayerListView'u ekranda göstermek istemiyorsanız, sadece bunu yapın:
        playerListView.setVisible(false);
        playerListView.setManaged(false);
    }


    private VBox createRoleInfoBox() {
        VBox roleBox = new VBox(5);
        roleBox.setPadding(new Insets(10));
        roleBox.setAlignment(Pos.CENTER);
        roleBox.getStyleClass().add("role-info-box");

        // Rol avatarı
        roleAvatarView = new ImageView();
        roleAvatarView.setFitWidth(50);
        roleAvatarView.setFitHeight(50);
        roleAvatarView.setPreserveRatio(true);

        // Avatar arka planı (yuvarlak şekil için)
        StackPane avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("role-avatar-container");
        avatarContainer.getChildren().add(roleAvatarView);

        // Rol adı etiketi
        roleNameLabel = new Label("Rol: ???");
        roleNameLabel.getStyleClass().add("role-name-label");

        roleBox.getChildren().addAll(avatarContainer, roleNameLabel);

        return roleBox;
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

    // Rol avatarını güncelleme
    // GameView sınıfında, updateRoleAvatar metodunu daha sağlam hale getirin
    public void updateRoleAvatar(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            roleName = "unknown"; // Varsayılan değer
        }

        roleNameLabel.setText("Rol: " + roleName);

        // Rol avatarını yükle
        String avatarPath = "/images/role_avatars/town.png"; // Varsayılan

        switch (roleName.toLowerCase()) {
            case "mafya":
                avatarPath = "/images/role_avatars/mafia.png";
                break;
            case "serif":
                avatarPath = "/images/role_avatars/sheriff.png";
                break;
            case "doktor":
                avatarPath = "/images/role_avatars/doctor.png";
                break;
            case "gardiyan":
                avatarPath = "/images/role_avatars/jailor.png";
                break;
            case "jester":
                avatarPath = "/images/role_avatars/jester.png";
                break;
        }

        try {
            Image roleImage = new Image(getClass().getResourceAsStream(avatarPath));
            if (roleImage.isError()) {
                System.err.println("Rol avatarı yüklenemedi: " + avatarPath);
                roleImage = new Image(getClass().getResourceAsStream("/images/role_avatars/town.png"));
            }
            roleAvatarView.setImage(roleImage);
        } catch (Exception e) {
            System.err.println("Rol avatarı yüklenirken hata: " + e.getMessage());
            try {
                // Varsayılan avatara geri dön
                roleAvatarView.setImage(new Image(getClass().getResourceAsStream("/images/role_avatars/town.png")));
            } catch (Exception ex) {
                System.err.println("Varsayılan avatar da yüklenemedi!");
            }
        }
    }

    // PlayerCircleView erişimcisi
    public PlayerCircleView getPlayerCircleView() {
        return playerCircleView;
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

        // Hapishane sohbeti sekmesi - isim değiştirildi
        jailChatTab = new Tab("Hapishane Hücresi"); // "Hapishane" yerine "Hapishane Hücresi"
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
        // Direct text update without any layout changes or other UI elements
        timeText.setText("SÜRE: " + seconds + " sn");

        // Only change color if the time is getting low (optional visual indicator)
        if (seconds <= 5) {
            timeText.setFill(Color.RED);
        } else if (seconds <= 10) {
            timeText.setFill(Color.ORANGE);
        } else {
            timeText.setFill(Color.BLACK);
        }
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

    public void ensureJailChatVisible() {
        Platform.runLater(() -> {
            try {
                jailChatTab.setDisable(false);
                // Tab'ı seçmiyoruz, sadece görünür yapıyoruz
                System.out.println("Hapishane sohbet tab'ı aktifleştirildi");
            } catch (Exception e) {
                System.err.println("Hapishane tab'ı gösterilirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public VoiceControlPanel getVoiceControlPanel() {
        return voiceControlPanel;
    }
}