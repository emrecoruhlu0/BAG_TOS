package com.bag_tos.client.view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Sohbet mesajlarını gösterme ve gönderme için yeniden kullanılabilir bileşen
 */
public class ChatPanel extends VBox {
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;

    /**
     * Varsayılan sohbet paneli oluşturur
     */
    public ChatPanel() {
        this("Mesajınızı yazın...");
    }

    /**
     * Özel ipucu metni ile sohbet paneli oluşturur
     *
     * @param promptText Mesaj alanında gösterilecek ipucu metni
     */
    public ChatPanel(String promptText) {
        setPadding(new Insets(10));
        setSpacing(10);
        getStyleClass().add("chat-panel");

        // Sohbet gösterim alanı
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(300);
        chatArea.getStyleClass().add("chat-area");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // Mesaj giriş alanı
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER);

        messageField = new TextField();
        messageField.setPromptText(promptText);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton = new Button("Gönder");
        sendButton.setDefaultButton(true);

        messageBox.getChildren().addAll(messageField, sendButton);

        getChildren().addAll(chatArea, messageBox);

        // Varsayılan olay işleyicileri
        messageField.setOnAction(e -> sendMessage());
        sendButton.setOnAction(e -> sendMessage());
    }

    /**
     * Mesaj gönderme olayı için özel işleyici ekler
     *
     * @param handler Mesaj gönderme işleyicisi
     */
    public void setOnSendMessage(MessageSendHandler handler) {
        this.sendHandler = handler;
    }

    /**
     * Sohbet alanına mesaj ekler
     *
     * @param message Eklenecek mesaj
     */
    public void addMessage(String message) {
        chatArea.appendText(message + "\n");
        // Otomatik kaydırma - en alt mesaja odaklanma
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Sohbet alanındaki tüm mesajları temizler
     */
    public void clearMessages() {
        chatArea.clear();
    }

    /**
     * Mesaj alanının metnini döndürür
     *
     * @return Mesaj alanındaki metin
     */
    public String getMessage() {
        return messageField.getText();
    }

    /**
     * Mesaj alanını temizler
     */
    public void clearMessageField() {
        messageField.clear();
    }

    /**
     * Mesaj gönderme durumunda bileşenin kendi iç işlevselliği
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && sendHandler != null) {
            sendHandler.onSend(message);
            messageField.clear();
        }
    }

    /**
     * Mesaj gönderme olayı için arayüz
     */
    private MessageSendHandler sendHandler;

    public interface MessageSendHandler {
        void onSend(String message);
    }

    // Getter metodları

    public TextArea getChatArea() {
        return chatArea;
    }

    public TextField getMessageField() {
        return messageField;
    }

    public Button getSendButton() {
        return sendButton;
    }
}