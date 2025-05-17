package com.bag_tos.client.view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class VoiceControlPanel extends HBox {
    private Button microphoneButton;
    private boolean microphoneActive = false;
    private MicrophoneStateChangeListener listener;

    private Image microphoneOnImage;
    private Image microphoneOffImage;

    public VoiceControlPanel() {
        // Panel ayarları
        setAlignment(Pos.CENTER_RIGHT);
        setPadding(new Insets(5));
        setSpacing(10);
        getStyleClass().add("voice-control-panel");

        // Görselleri yükle
        loadImages();

        // Mikrofon butonu
        microphoneButton = new Button();
        microphoneButton.setGraphic(new ImageView(microphoneOffImage));
        microphoneButton.setMaxSize(30, 30); // Butonu da sınırla
        microphoneButton.setMinSize(30, 30);
        microphoneButton.setPrefSize(30, 30);
        microphoneButton.setTooltip(new Tooltip("Mikrofonu Aç/Kapat"));
        microphoneButton.getStyleClass().add("microphone-button");

        // Buton tıklama olayı
        microphoneButton.setOnAction(e -> toggleMicrophone());

        // Panele ekle
        getChildren().add(microphoneButton);

        // Başlangıçta pasif
        updateMicrophoneButton();
    }

    private void loadImages() {
        try {
            int iconSize = 24; // 24x24 piksel veya istediğiniz boyut

            microphoneOnImage = new Image(getClass().getResourceAsStream("/images/microphone.png"),
                    iconSize, iconSize, true, true);
            microphoneOffImage = new Image(getClass().getResourceAsStream("/images/microphone_off.png"),
                    iconSize, iconSize, true, true);

        } catch (Exception e) {
            System.err.println("Mikrodon görselleri yüklenemedi: " + e.getMessage());
            // Dummy görseller oluştur - boş URL ile 1x1 görsel
            microphoneOnImage = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAEBgIApD5fRAAAAABJRU5ErkJggg==");
            microphoneOffImage = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAEBgIApD5fRAAAAABJRU5ErkJggg==");
        }
    }

    private void toggleMicrophone() {
        microphoneActive = !microphoneActive;
        updateMicrophoneButton();

        // Dinleyici varsa bildirimi gönder
        if (listener != null) {
            listener.onMicrophoneStateChanged(microphoneActive);
        }
    }

    private void updateMicrophoneButton() {
        if (microphoneActive) {
            // Mikrofon aktif
            ((ImageView) microphoneButton.getGraphic()).setImage(microphoneOnImage);
            microphoneButton.setTooltip(new Tooltip("Mikrofonu Kapat"));
            microphoneButton.getStyleClass().add("microphone-active");
            microphoneButton.getStyleClass().remove("microphone-inactive");
        } else {
            // Mikrofon pasif
            ((ImageView) microphoneButton.getGraphic()).setImage(microphoneOffImage);
            microphoneButton.setTooltip(new Tooltip("Mikrofonu Aç"));
            microphoneButton.getStyleClass().add("microphone-inactive");
            microphoneButton.getStyleClass().remove("microphone-active");
        }
    }

    public void setMicrophoneActive(boolean active) {
        if (microphoneActive != active) {
            microphoneActive = active;
            updateMicrophoneButton();
        }
    }

    public boolean isMicrophoneActive() {
        return microphoneActive;
    }

    public void setMicrophoneEnabled(boolean enabled) {
        microphoneButton.setDisable(!enabled);

        if (!enabled) {
            microphoneActive = false;
            updateMicrophoneButton();
        }
    }

    public void setMicrophoneStateChangeListener(MicrophoneStateChangeListener listener) {
        this.listener = listener;
    }

    public interface MicrophoneStateChangeListener {
        void onMicrophoneStateChanged(boolean active);
    }
}