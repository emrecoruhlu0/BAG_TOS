package com.bag_tos.common.message.response;

public class ChatMessageResponse {
    private String sender;
    private String message;
    private String room;  // "LOBBY" veya "MAFIA" vb.

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(String sender, String message, String room) {
        this.sender = sender;
        this.message = message;
        this.room = room;
    }

    // Getter ve Setter metodları
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}