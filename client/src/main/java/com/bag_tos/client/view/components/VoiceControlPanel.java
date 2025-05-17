package com.bag_tos.client.view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Ses kontrolü için kullanıcı arayüzü bileşeni.
 * Mikrofon açma/kapama butonu içerir.
 */
public class VoiceControlPanel extends HBox {
    private Button microphoneButton;
    private boolean microphoneActive = false;
    private MicrophoneStateChangeListener listener;

    private Image microphoneOnImage;
    private Image microphoneOffImage;

    /**
     * Ses kontrol paneli oluşturur
     */
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
        microphoneButton.setTooltip(new Tooltip("Mikrofonu Aç/Kapat"));
        microphoneButton.getStyleClass().add("microphone-button");

        // Buton tıklama olayı
        microphoneButton.setOnAction(e -> toggleMicrophone());

        // Panele ekle
        getChildren().add(microphoneButton);

        // Başlangıçta pasif
        updateMicrophoneButton();
    }

    /**
     * Görselleri yükler
     */
    private void loadImages() {
        try {
            microphoneOnImage = new Image(getClass().getResourceAsStream("/images/microphone.png"));
            microphoneOffImage = new Image(getClass().getResourceAsStream("/images/microphone_off.png"));
        } catch (Exception e) {
            System.err.println("Mikrodon görselleri yüklenemedi: " + e.getMessage());
            // Dummy görseller oluştur
            microphoneOnImage = new Image(1, 1, false, false);
            microphoneOffImage = new Image(1, 1, false, false);
        }
    }

    /**
     * Mikrofonun aktifliğini değiştirir
     */
    private void toggleMicrophone() {
        microphoneActive = !microphoneActive;
        updateMicrophoneButton();

        // Dinleyici varsa bildirimi gönder
        if (listener != null) {
            listener.onMicrophoneStateChanged(microphoneActive);
        }
    }

    /**
     * Mikrofon butonunun görünümünü günceller
     */
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

    /**
     * Mikrofonun aktifliğini harici olarak ayarlar
     * @param active Aktif ise true
     */
    public void setMicrophoneActive(boolean active) {
        if (microphoneActive != active) {
            microphoneActive = active;
            updateMicrophoneButton();
        }
    }

    /**
     * Mikrofonun aktif olup olmadığını döndürür
     * @return Aktif ise true
     */
    public boolean isMicrophoneActive() {
        return microphoneActive;
    }

    /**
     * Mikrofon butonu aktifliğini ayarlar
     * @param enabled Aktif ise true
     */
    public void setMicrophoneEnabled(boolean enabled) {
        microphoneButton.setDisable(!enabled);

        if (!enabled) {
            microphoneActive = false;
            updateMicrophoneButton();
        }
    }

    /**
     * Mikrofon durumu değişim dinleyicisi ayarlar
     * @param listener Dinleyici
     */
    public void setMicrophoneStateChangeListener(MicrophoneStateChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Mikrofon durumu değişim dinleyici arayüzü
     */
    public interface MicrophoneStateChangeListener {
        void onMicrophoneStateChanged(boolean active);
    }
}