package com.bag_tos.client.view;

import com.bag_tos.client.view.components.AvatarSelector;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class LoginView extends VBox {
    // Mevcut değişkenler
    private TextField usernameField;
    private TextField serverField;
    private TextField portField;
    private Button connectButton;
    private Text statusText;

    // Yeni avatar seçici değişkeni
    private AvatarSelector avatarSelector;

    public LoginView() {
        // Mevcut yapılandırma
        setPadding(new Insets(20));
        setSpacing(10);
        setAlignment(Pos.CENTER);

        // Başlık
        Text titleText = new Text("Town of Salem Clone");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 24));

        // Giriş formu
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label usernameLabel = new Label("Kullanıcı Adı:");
        usernameField = new TextField();
        usernameField.setPromptText("Kullanıcı adınızı girin");

        Label serverLabel = new Label("Sunucu:");
        serverField = new TextField("localhost");

        Label portLabel = new Label("Port:");
        portField = new TextField("1234");

        // Avatar seçici oluştur
        Label avatarLabel = new Label("Avatar Seçin:");
        avatarSelector = new AvatarSelector();

        grid.add(usernameLabel, 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(serverLabel, 0, 1);
        grid.add(serverField, 1, 1);
        grid.add(portLabel, 0, 2);
        grid.add(portField, 1, 2);
        grid.add(avatarLabel, 0, 3);
        grid.add(avatarSelector, 1, 3);

        // Bağlantı butonu
        connectButton = new Button("Bağlan");
        connectButton.setPrefWidth(150);

        // Durum mesajı
        statusText = new Text();
        statusText.setFont(Font.font("System", 12));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().add(connectButton);

        getChildren().addAll(titleText, grid, buttonBox, statusText);
    }

    public String getSelectedAvatarId() {
        return avatarSelector.getSelectedAvatarId();
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public TextField getServerField() {
        return serverField;
    }

    public TextField getPortField() {
        return portField;
    }

    public Button getConnectButton() {
        return connectButton;
    }

    public void setStatusText(String text) {
        statusText.setText(text);
    }
}