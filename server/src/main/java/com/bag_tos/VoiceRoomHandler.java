package com.bag_tos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ses iletişimi için oda yönetimi.
 * Sadece LOBBY odasında ve hayatta olan oyuncular arasında sesli iletişime izin verir.
 */
public class VoiceRoomHandler {
    private final RoomHandler gameRoomHandler;

    // Ses odaları ve katılımcıları
    private final Set<String> voiceParticipants = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Oyuncu durumları
    private final Map<String, Boolean> playerAliveStatus = new ConcurrentHashMap<>();

    // Oyun fazı
    private boolean isNightPhase = false;

    /**
     * Ses oda yöneticisi oluşturur
     * @param gameRoomHandler Oyun oda yöneticisi
     */
    public VoiceRoomHandler(RoomHandler gameRoomHandler) {
        this.gameRoomHandler = gameRoomHandler;
    }

    /**
     * Oyuncunun konuşabilip konuşamayacağını kontrol eder
     * @param username Kullanıcı adı
     * @param roomName Oda adı (Bu parametre basitleştirilmiş versiyonda önemsizdir çünkü sadece LOBBY kullanılır)
     * @return Konuşabilirse true
     */
    public boolean canTalk(String username, String roomName) {
        // Sadece LOBBY odasında konuşmaya izin ver
        if (!"LOBBY".equals(roomName)) {
            return false;
        }

        // Oyuncu hayatta mı
        Boolean alive = playerAliveStatus.get(username);
        if (alive != null && !alive) {
            return false;
        }

        // Gece fazında mı
        if (isNightPhase) {
            return false;
        }

        // Oyuncu sesli sohbete katılıyor mu
        return voiceParticipants.contains(username);
    }

    /**
     * Oyuncunun dinleyebilip dinleyemeyeceğini kontrol eder
     * @param username Kullanıcı adı
     * @param roomName Oda adı
     * @return Dinleyebilirse true
     */
    public boolean canListen(String username, String roomName) {
        // Sadece LOBBY odasında dinlemeye izin ver
        if (!"LOBBY".equals(roomName)) {
            return false;
        }

        // Oyuncu hayatta mı
        Boolean alive = playerAliveStatus.get(username);
        if (alive != null && !alive) {
            return false;
        }

        // Oyuncu sesli sohbete katılıyor mu
        return voiceParticipants.contains(username);
    }

    /**
     * Oyuncu ses sistemine katıldığında çağrılır
     * @param username Kullanıcı adı
     * @param roomName Oda adı (Bu parametre sadece LOBBY olmalıdır)
     */
    public void playerJoinedRoom(String username, String roomName) {
        // Sadece LOBBY odasına katılıma izin ver
        if ("LOBBY".equals(roomName)) {
            voiceParticipants.add(username);

            // Oyuncu durumunu ayarla (varsayılan: hayatta)
            playerAliveStatus.putIfAbsent(username, true);

            System.out.println("Voice: " + username + " joined voice chat");
        }
    }

    /**
     * Oyuncu ses sisteminden ayrıldığında çağrılır
     * @param username Kullanıcı adı
     * @param roomName Oda adı
     */
    public void playerLeftRoom(String username, String roomName) {
        if ("LOBBY".equals(roomName)) {
            voiceParticipants.remove(username);
            System.out.println("Voice: " + username + " left voice chat");
        }
    }

    /**
     * Oyuncu öldüğünde çağrılır
     * @param username Kullanıcı adı
     */
    public void playerDied(String username) {
        playerAliveStatus.put(username, false);
        System.out.println("Voice: " + username + " is now dead (can't talk/listen)");
    }

    /**
     * Oyun fazı değiştiğinde çağrılır
     * @param nightPhase Gece fazı mı
     */
    public void setNightPhase(boolean nightPhase) {
        this.isNightPhase = nightPhase;
        System.out.println("Voice: Phase changed to " + (nightPhase ? "NIGHT" : "DAY"));
    }

    /**
     * Sesli sohbette aktif olan oyuncuların listesini döndürür
     * @param roomName Oda adı (sadece LOBBY için çalışır)
     * @return Sesli sohbetteki oyuncuların listesi
     */
    public List<String> getPlayersInRoom(String roomName) {
        if ("LOBBY".equals(roomName)) {
            return new ArrayList<>(voiceParticipants);
        }
        return Collections.emptyList();
    }
}