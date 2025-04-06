package com.bag_tos;

public class MessageUtils {
    // Sıfırlama
    public static final String RESET = "\033[0m";

    // Metin Renkleri
    public static final String SIYAH = "\033[30m";
    public static final String KIRMIZI = "\033[31m";
    public static final String YESIL = "\033[32m";
    public static final String SARI = "\033[33m";
    public static final String MAVI = "\033[34m";
    public static final String MOR = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String BEYAZ = "\033[37m";
    private static final String GRİ = "\033[90m";

    // Arka Plan Renkleri
    public static final String BG_SIYAH = "\033[40m";
    public static final String BG_KIRMIZI = "\033[41m";
    public static final String BG_YESIL = "\033[42m";
    public static final String BG_SARI = "\033[43m";
    public static final String BG_MAVI = "\033[44m";
    public static final String BG_MOR = "\033[45m";
    public static final String BG_CYAN = "\033[46m";
    public static final String BG_BEYAZ = "\033[47m";

    // Stiller
    public static final String BOLD = "\033[1m";    // Kalın
    public static final String ITALIC = "\033[3m";  // İtalik
    public static final String UNDERLINE = "\033[4m";// Altı çizili

    // Faz Bilgisi için Sabitler
    public static final String PHASE_NIGHT = "GECE";
    public static final String PHASE_DAY = "GÜNDÜZ";

    // Debug Modu Kontrolü
    private static boolean debugMode = false;

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    // Renkli Mesaj Oluşturma
    public static String formatMessage(String message){
        String formattedMessage = KIRMIZI + "----------------------------";
        formattedMessage += "\n" + message + "\n";
        formattedMessage += "----------------------------" + RESET;

        return formattedMessage;
    }

    public static String formatMessage(String message, Game.Phase phase, boolean isDebug) {
        String formattedMessage = "";

        // Faz'a Göre Renk
        if (phase.equals(Game.Phase.NIGHT)) {
            formattedMessage += BG_MAVI + "[GECE] ";
        } else if (phase.equals(Game.Phase.DAY)) {
            formattedMessage += BG_SARI + "[GÜNDÜZ] ";
        }

        // Debug Mesajları
        if (isDebug && debugMode) {
            formattedMessage += GRİ + "[DEBUG] ";
        }

        // Mesaj ve Reset
        formattedMessage += message + RESET;
        return formattedMessage;
    }

    public static String formatMessage(String message_first, String message_second, String except, Game.Phase phase, boolean isDebug){
        String formattedMessage = "";
        //String Renk parametresi ile hariç kısmın rengi de belirlenebilir

        // Faz'a Göre Renk
        if (phase.equals(Game.Phase.NIGHT)) {
            formattedMessage += BG_MAVI + "[GECE]";
            formattedMessage += message_first + RESET + " " + BG_KIRMIZI + except + RESET + " " + BG_MAVI + message_second + RESET;
        } else if (phase.equals(Game.Phase.DAY)) {
            formattedMessage += BG_SARI + "[GÜNDÜZ]";
            formattedMessage += message_first + RESET + " " + BG_KIRMIZI +  except +  RESET + " " + BG_SARI + message_second + RESET;
        }

//        // Debug Mesajları
//        if (isDebug && debugMode) {
//            formattedMessage += GRİ + "[DEBUG] ";
//        }
//
//        // Mesaj ve Reset
//        formattedMessage += message + RESET;
        return formattedMessage;
    }

    public static String asilmaMessage(){
        return formatMessage("ASILDIN");
    }
    public static String olduruldunMessage(){
        return formatMessage("OLDURULDUN");
    }

    public static String formatError(String message){
        return KIRMIZI + "✖ " + message + RESET;
    }

    public static String formatSuccess(String message) {
        return YESIL + "✔ " + message + RESET;
    }

    public static String formatWarning(String message) {
        return SARI + "⚠ " + message + RESET;
    }

    // Direkt Yazdırma için Kısayol
    public static void print(String message, Game.Phase phase, boolean isDebug) {
        System.out.println(formatMessage(message, phase, isDebug));
    }
}