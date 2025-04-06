package com.bag_tos;

import com.bag_tos.roles.Role;

import java.util.*;
import java.util.concurrent.*;

import static com.bag_tos.MessageUtils.*;

public class Game {
    private List<ClientHandler> players;
    private Map<String, Role> roles;
    private List<String> alivePlayers;
    private Map<String, String> nightActions = new ConcurrentHashMap<>();
    private Map<String, String> votes;

    private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> countdownTask;
    private int remainingSeconds;
    private Phase currentPhase;
    private RoomHandler roomHandler;

    public Game(List<ClientHandler> players) {
        this.players = players;
        this.roles = new HashMap<>();
        this.alivePlayers = new ArrayList<>();
        this.nightActions = new HashMap<>();
        this.votes = new HashMap<>();
        this.timer = Executors.newScheduledThreadPool(1);
        //this.roomHandler = new RoomHandler();
        //initializeGame();
    }

    public void setRoomHandler(RoomHandler roomHandler) {
        this.roomHandler = roomHandler;
    }

    public RoomHandler getRoomHandler() {
        return roomHandler;
    }

    public Object getRole(String username) {
        return roles.get(username);
    }

    public enum Phase {
        NIGHT, DAY;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void start() {

//        for (ClientHandler player : players) {
//            roomHandler.addToRoom("LOBBY", player);
//        }

//        players.stream()
//                .filter(p -> !(roles.get(p.getUsername()) instanceof Mafya))
//                .forEach(p -> {
//                    //roomHandler.removeFromRoom("MAFYA", p);
//                    roomHandler.addToRoom("LOBBY", p);
//                });

        players.stream()
                .filter(p -> roles.get(p.getUsername()) instanceof Mafya)
                .forEach(p -> roomHandler.addToRoom("MAFYA", p));
        startNightPhase();
    }

    public void initializeGame() {
        for (ClientHandler player : players) {
            alivePlayers.add(player.getUsername());
        }
        assignRoles();
    }

    public void assignRoles() {
        List<Role> rolePool = new ArrayList<>();
        int playerCount = players.size();

        // Rol dağılımı (Town of Salem formatı)
        rolePool.add(new Mafya()); // Godfather
        //rolePool.add(new Framer());
        rolePool.add(new Serif());
        rolePool.add(new Doktor());
        rolePool.add(new Jester());

        // Oyuncu sayısına göre rollerin dengelenmesi
        if (playerCount > 5) {
            //rolePool.add(new Bodyguard());
            //rolePool.add(new Executioner());
        }

        Collections.shuffle(rolePool);

        // Oyunculara rollerin atanması
        for (int i = 0; i < players.size(); i++) {
            String username = players.get(i).getUsername();
            Role role = rolePool.get(i);
            roles.put(username, role);
            System.out.println(YESIL + "[DEBUG] Rol Atama: " + username + " -> " + role.getName() + RESET);
        }
    }
    private void startNightPhase() {
        currentPhase = Phase.NIGHT;
        remainingSeconds = 30;
        for (ClientHandler player : players) {
            //roomHandler.removeFromRoom("LOBBY", player);
//            if (isMafia(player)) {
//                // Mafya üyelerini MAFYA odasına ekle (zaten ekliyse tekrar ekleme)
//                roomHandler.addToRoom("MAFYA", player);
//            }
        }
        broadcastToAlivePlayers(formatMessage("Gece başladı!", currentPhase, true));
        startCountdown();
        scheduleNightActions();
        nightActions.clear();

        for (ClientHandler player : players) {
            String username = player.getUsername();
            Role role = roles.get(username);
            if (role instanceof Mafya) {
                player.sendMessage("AKSIYON: /oldur <oyuncu>");
            } else if (role instanceof Doktor) {
                player.sendMessage("AKSIYON: /iyilestir <oyuncu>");
            }
        }
    }

    private boolean isMafia(ClientHandler player) {
        return roles.get(player.getUsername()) instanceof Mafya;
    }

    private void processNightActions() {
        String mafyaHedef = null;
        String doktorHedef = null;
        System.out.println("Gece aksiyonları işleniyor...");
        System.out.println("NightActions Map: " + nightActions.toString());

        for (Map.Entry<String, String> entry : nightActions.entrySet()) {
            String oyuncu = entry.getKey();
            String hedef = entry.getValue();
            Role role = roles.get(oyuncu);

            if (role instanceof Mafya) {
                mafyaHedef = hedef;
            } else if (role instanceof Doktor) {
                doktorHedef = hedef;
            }
        }

        if (mafyaHedef != null && !mafyaHedef.equals(doktorHedef)) {
            alivePlayers.remove(mafyaHedef);
            String finalMafyaHedef = mafyaHedef;
            players.stream()
                    .filter(p -> p.getUsername().equals(finalMafyaHedef))
                    .forEach(p -> {
                        p.setAlive(false);
                        p.sendMessage(olduruldunMessage());
                    });
            //System.out.println("[DEBUG] Mafya hedefi: " + mafyaHedef);
            //System.out.println("[DEBUG] Doktor hedefi: " + doktorHedef);
            broadcastToAlivePlayers(formatMessage("", "olduruldu", mafyaHedef, currentPhase, true));
        }
        checkWinConditions();
    }

    private void startDayPhase() {
        currentPhase = Phase.DAY;
        remainingSeconds = 30;
//        players.stream()
//                //.filter(p -> !(roles.get(p.getUsername()) instanceof Mafya))
//                .forEach(p -> {
//                    //roomHandler.removeFromRoom("MAFYA", p);
//                    //roomHandler.addToRoom("LOBBY", p);
//                });
//
//        players.stream()
//                .filter(p -> roles.get(p.getUsername()) instanceof Mafya)
//                .forEach(p -> roomHandler.addToRoom("MAFYA", p));

        broadcastToAlivePlayers(formatMessage("Gündüz başladı!", "saniye kaldı.", String.valueOf(remainingSeconds), currentPhase, true));
        startCountdown();
        scheduleDayActions();
        votes.clear();

    }

    private void processVotes() {
        Map<String, Integer> oySayilari = new HashMap<>();
        for (String hedef : votes.values()) {
            oySayilari.put(hedef, oySayilari.getOrDefault(hedef, 0) + 1);
        }

        String asilan = null;
        int maxOy = 0;
        for (Map.Entry<String, Integer> entry : oySayilari.entrySet()) {
            if (entry.getValue() > maxOy) {
                maxOy = entry.getValue();
                asilan = entry.getKey();
            }
        }

        if (asilan != null) {
            alivePlayers.remove(asilan);
            String finalAsilan = asilan;
            players.stream()
                    .filter(p -> p.getUsername().equals(finalAsilan))
                    .forEach(p -> {
                        p.setAlive(false);
                        p.sendMessage(asilmaMessage());
                    });
            broadcastToAlivePlayers(formatMessage("", "asildi!", asilan, currentPhase, true));

        }

        checkWinConditions();
    }

    private void checkWinConditions() {
        int mafyaCount = 0;
        int othersCount = 0;

        for (String username : alivePlayers) {
            Role role = roles.get(username);
            if (role instanceof Mafya) {
                mafyaCount++;
            } else {
                othersCount++;
            }
        }

        if (mafyaCount == 0) {
            endGame("Köylü");
        } else if (mafyaCount >= othersCount) {
            endGame("Mafya");
        }
    }

    private void endGame(String kazanan) {
        timer.shutdownNow(); // Tüm zamanlayıcı görevlerini durdur

        String kazananMesaj;
        if (kazanan.equalsIgnoreCase("Mafya")) {
            kazananMesaj = BG_KIRMIZI + BOLD + "\n=== OYUN BİTTİ ===\nMAFYA KAZANDI!\n" + RESET;
        } else if (kazanan.equalsIgnoreCase("Köylü")) {
            kazananMesaj = BG_YESIL + BOLD + "\n=== OYUN BİTTİ ===\nKÖYLÜLER KAZANDI!\n" + RESET;
        } else {
            kazananMesaj = BG_BEYAZ + BOLD + "\n=== OYUN BİTTİ ===\n" + kazanan.toUpperCase() + " kazandı!\n" + RESET;
        }

        players.forEach(p -> p.sendMessage(kazananMesaj));
    }



    private void broadcastToAlivePlayers(String message) {
        players.stream()
                .filter(ClientHandler::isAlive)
                .forEach(p -> p.sendMessage(message));
    }

    public void handleAction(String oyuncu, String komut) {
        //System.out.println("[DEBUG] HandleAction: " + oyuncu + " -> " + komut);
        if (komut.startsWith("/oldur ")) {
            String[] parcalar = komut.split(" ", 2); // Boşluğa göre böl
            if (parcalar.length >= 2) {
                String hedef = parcalar[1];
                nightActions.put(oyuncu, hedef);
                System.out.println("[DEBUG] Mafya hedefi eklendi: " + oyuncu + " -> " + hedef);
            }
        } else if (komut.startsWith("/iyilestir ")) {
            String[] parcalar = komut.split(" ", 2); // Boşluğa göre böl
            if (parcalar.length >= 2) {
                String hedef = parcalar[1];
                nightActions.put(oyuncu, hedef);
                System.out.println("[DEBUG] Doktor hedefi eklendi: " + oyuncu + " -> " + hedef);
            }
        }
    }

    public void handleVote (String oyuncu, String hedef){
        votes.put(oyuncu, hedef);
        System.out.println("[DEBUG] Oy verildi: " + oyuncu + " -> " + hedef);
    }

    private void startCountdown() {
        // Önceki geri sayımı iptal et
        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(true);
        }

        // Yeni geri sayımı başlat
        countdownTask = timer.scheduleAtFixedRate(() -> {
            remainingSeconds--;
            System.out.print("\rKalan süre: " + remainingSeconds + " saniye"); // Aynı satırda güncelle

            if (remainingSeconds <= 0) {
                System.out.println(); // Yeni satıra geç
                countdownTask.cancel(true);
            }
        }, 1, 1, TimeUnit.SECONDS); // 1 saniye gecikme, 1 saniye aralık
    }

    private void scheduleNightActions() {
        timer.schedule(() -> {
            processNightActions();
            startDayPhase();
        }, 30, TimeUnit.SECONDS);
    }

    private void scheduleDayActions() {
        timer.schedule(() -> {
            processVotes();
            startNightPhase();
        }, 30, TimeUnit.SECONDS);
    }

    public void addPlayer(ClientHandler player){
        players.add(player);
    }
}
