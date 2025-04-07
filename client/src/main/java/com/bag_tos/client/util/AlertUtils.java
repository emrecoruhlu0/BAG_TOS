package com.bag_tos.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Uyarı diyalogları oluşturmak için yardımcı sınıf
 */
public class AlertUtils {

    /**
     * Hata mesajı gösteren bir diyalog
     *
     * @param title Diyalog başlığı
     * @param header Diyalog üst başlık
     * @param content Mesaj içeriği
     * @return Tıklanan buton
     */
    public static ButtonType showError(String title, String header, String content) {
        return showAlert(Alert.AlertType.ERROR, title, header, content);
    }

    /**
     * Bilgi mesajı gösteren bir diyalog
     *
     * @param title Diyalog başlığı
     * @param header Diyalog üst başlık
     * @param content Mesaj içeriği
     * @return Tıklanan buton
     */
    public static ButtonType showInfo(String title, String header, String content) {
        return showAlert(Alert.AlertType.INFORMATION, title, header, content);
    }

    /**
     * Uyarı mesajı gösteren bir diyalog
     *
     * @param title Diyalog başlığı
     * @param header Diyalog üst başlık
     * @param content Mesaj içeriği
     * @return Tıklanan buton
     */
    public static ButtonType showWarning(String title, String header, String content) {
        return showAlert(Alert.AlertType.WARNING, title, header, content);
    }

    /**
     * Onay mesajı gösteren bir diyalog
     *
     * @param title Diyalog başlığı
     * @param header Diyalog üst başlık
     * @param content Mesaj içeriği
     * @return Tıklanan buton
     */
    public static ButtonType showConfirmation(String title, String header, String content) {
        return showAlert(Alert.AlertType.CONFIRMATION, title, header, content);
    }

    /**
     * Genel diyalog gösterme metodu
     */
    private static ButtonType showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        if (title != null) alert.setTitle(title);
        if (header != null) alert.setHeaderText(header);
        if (content != null) alert.setContentText(content);

        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    /**
     * Belirli bir Stage'e ait diyalog oluşturur
     */
    public static Alert createAlert(Alert.AlertType type, Stage owner, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        if (title != null) alert.setTitle(title);
        if (header != null) alert.setHeaderText(header);
        if (content != null) alert.setContentText(content);

        return alert;
    }
}