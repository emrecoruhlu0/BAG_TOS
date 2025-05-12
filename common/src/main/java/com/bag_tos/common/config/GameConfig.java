package com.bag_tos.common.config;

/**
 * Oyun yapılandırma ayarları (Ortak)
 */
public class GameConfig {
    // Oyuncu limitleri
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 8;

    // Zaman limitleri (saniye)
    public static final int NIGHT_PHASE_DURATION = 30;
    public static final int DAY_PHASE_DURATION = 30;
    public static final int DISCUSSION_PHASE_DURATION = 60;

    // Rol yapılandırması
    public static final int MAX_MAFIA_RATIO = 3; // Her 3 oyuncuya 1 mafya

    // Oyun mekanikleri
    public static final boolean REVEAL_ROLES_ON_DEATH = false;
    public static final boolean ALLOW_DEAD_CHAT = false;
    public static final boolean ALLOW_SELF_ACTIONS = false; // Kendi üzerinde aksiyon yapma

    // Oyun ekranı ayarları
    public static final boolean DISABLE_CHAT_AT_NIGHT = true;

    // Anti-hile
    public static final int MAX_INVALID_MESSAGES = 5;
    public static final int RECONNECT_TIMEOUT_SECONDS = 30;
}