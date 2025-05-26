package com.bag_tos.common.config;

/**
 * Oyun yapılandırma ayarları (Sadece Zaman Ayarları Değiştirilebilir)
 */
public class GameConfig {
    // SABİT AYARLAR (Değişmez)
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 8;
    public static final int DISCUSSION_PHASE_DURATION = 60;
    public static final int MAX_MAFIA_RATIO = 3;
    public static final boolean REVEAL_ROLES_ON_DEATH = false;
    public static final boolean ALLOW_DEAD_CHAT = false;
    public static final boolean ALLOW_SELF_ACTIONS = false;
    public static final boolean DISABLE_CHAT_AT_NIGHT = true;
    public static final int MAX_INVALID_MESSAGES = 5;
    public static final int RECONNECT_TIMEOUT_SECONDS = 30;
    public static final boolean NOTIFY_HEALED_PLAYERS = true;
    public static final boolean JAILOR_CAN_SPEAK_TO_DEAD = false;

    public static final double MAFIA_RATIO_MIN = 0.15; // Min %15 mafya
    public static final double MAFIA_RATIO_MAX = 0.35; // Max %35 mafya
    public static final boolean GUARANTEE_CORE_ROLES = true;

    // DEĞİŞTİRİLEBİLİR ZAMAN AYARLARI
    private static volatile int customNightDuration = 30;
    private static volatile int customDayDuration = 30;

    // ESKİ STATIC ALANLAR (Geriye uyumluluk için)
    public static final int NIGHT_PHASE_DURATION = 30;  // Varsayılan değer
    public static final int DAY_PHASE_DURATION = 30;    // Varsayılan değer

    // ZAMAN AYARLARI İÇİN GETTER METODLARI
    public static int getCurrentNightDuration() {
        return customNightDuration;
    }

    public static int getCurrentDayDuration() {
        return customDayDuration;
    }

    // ZAMAN AYARLARI İÇİN SETTER METODLARI
    public static void setNightPhaseDuration(int value) {
        customNightDuration = Math.max(10, Math.min(value, 300));
    }

    public static void setDayPhaseDuration(int value) {
        customDayDuration = Math.max(10, Math.min(value, 300));
    }

    // Varsayılan değerlere dönme (sadece zaman ayarları)
    public static void resetTimeToDefaults() {
        customNightDuration = NIGHT_PHASE_DURATION;
        customDayDuration = DAY_PHASE_DURATION;
    }

    // Basit konfigürasyon özeti (sadece zaman)
    public static String getTimeConfigSummary() {
        return String.format(
                "Gece: %ds | Gündüz: %ds | Min Oyuncu: %d | Max Oyuncu: %d",
                customNightDuration, customDayDuration, MIN_PLAYERS, MAX_PLAYERS
        );
    }
}