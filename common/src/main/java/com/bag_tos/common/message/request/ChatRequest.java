package com.bag_tos.common.message.request;

public class ChatRequest {
    private String message;
    private String room;  // "LOBBY" veya "MAFIA" vb.

    public ChatRequest() {
    }

    public ChatRequest(String message, String room) {
        this.message = message;
        this.room = room;
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