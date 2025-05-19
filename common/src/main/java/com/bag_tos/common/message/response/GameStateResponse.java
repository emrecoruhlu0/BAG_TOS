package com.bag_tos.common.message.response;

import com.bag_tos.common.model.PlayerInfo;
import java.util.List;

public class GameStateResponse {
    private String phase;           // "NIGHT", "DAY", "LOBBY"
    private int remainingTime;      // Kalan süre (saniye)
    private List<PlayerInfo> players;  // Oyuncu listesi

    public GameStateResponse() {
    }

    public GameStateResponse(String phase, int remainingTime, List<PlayerInfo> players) {
        this.phase = phase;
        this.remainingTime = remainingTime;
        this.players = players;
    }

    public String getPhase() {
        return phase;
    }

}