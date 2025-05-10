package com.bag_tos.common.message.response;

import com.bag_tos.common.model.PlayerInfo;
import java.util.List;

/**
 * Oyun durumu bildirimi için yanıt sınıfı
 */
public class GameStateResponse {
    private String phase;           // "NIGHT", "DAY", "LOBBY"
    private int remainingTime;      // Kalan süre (saniye)
    private List<PlayerInfo> players;  // Oyuncu listesi

    // Boş constructor
    public GameStateResponse() {
    }

    // Constructor
    public GameStateResponse(String phase, int remainingTime, List<PlayerInfo> players) {
        this.phase = phase;
        this.remainingTime = remainingTime;
        this.players = players;
    }

    // Getter ve Setter metodları
    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }
}