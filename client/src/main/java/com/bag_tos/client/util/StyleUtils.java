package com.bag_tos.client.util;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UI stil yardımcıları
 */
public class StyleUtils {

    /**
     * Başlık stili uygular
     *
     * @param label Stil uygulanacak Label
     * @param size Font boyutu
     */
    public static void applyTitleStyle(Label label, int size) {
        label.setFont(Font.font("System", FontWeight.BOLD, size));
    }

    /**
     * Gece fazı stilini uygular
     *
     * @param node Stil uygulanacak Node
     */
    public static void applyNightPhaseStyle(Node node) {
        node.setStyle("-fx-background-color: #1a1a2e;");
    }

    /**
     * Gündüz fazı stilini uygular
     *
     * @param node Stil uygulanacak Node
     */
    public static void applyDayPhaseStyle(Node node) {
        node.setStyle("-fx-background-color: #ffd166;");
    }

    /**
     * TextArea'ya renkli metin ekler
     *
     * @param textArea Hedef TextArea
     * @param text Eklenecek metin
     * @param color Metin rengi
     */
    public static void appendColoredText(TextArea textArea, String text, Color color) {
        // JavaFX TextArea direkt olarak renkli metin desteklemez,
        // bu metot ileride HTMLEditor veya RichTextFX ile değiştirilebilir
        textArea.appendText(text + "\n");
    }

    /**
     * Birincil aksiyon butonuna stil uygular
     *
     * @param button Stil uygulanacak buton
     */
    public static void applyPrimaryButtonStyle(Button button) {
        button.getStyleClass().add("primary-button");
    }

    /**
     * İkincil aksiyon butonuna stil uygular
     *
     * @param button Stil uygulanacak buton
     */
    public static void applySecondaryButtonStyle(Button button) {
        button.getStyleClass().add("secondary-button");
    }

    /**
     * Tehlikeli aksiyon butonuna stil uygular
     *
     * @param button Stil uygulanacak buton
     */
    public static void applyDangerButtonStyle(Button button) {
        button.getStyleClass().add("danger-button");
    }
}