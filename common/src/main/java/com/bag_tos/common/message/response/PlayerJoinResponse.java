package com.bag_tos.common.message.response;

public class PlayerJoinResponse {
    private String username;
    private int totalPlayers;

    public PlayerJoinResponse() {
    }

    public PlayerJoinResponse(String username, int totalPlayers) {
        this.username = username;
        this.totalPlayers = totalPlayers;
    }

    // Getter ve Setter metodları
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }
}