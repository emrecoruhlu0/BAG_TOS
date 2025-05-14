package com.bag_tos;

import com.bag_tos.common.config.GameConfig;
import com.bag_tos.common.message.Message;
import com.bag_tos.common.message.MessageType;
import com.bag_tos.common.message.response.ActionResultResponse;
import com.bag_tos.common.message.response.GameStateResponse;
import com.bag_tos.common.message.response.RoleAssignmentResponse;
import com.bag_tos.common.model.ActionType;
import com.bag_tos.common.model.GamePhase;
import com.bag_tos.common.model.PlayerInfo;
import com.bag_tos.common.model.RoleType;
import com.bag_tos.common.util.JsonUtils;
import com.bag_tos.roles.Role;
import com.bag_tos.roles.mafia.Mafya;
import com.bag_tos.roles.naturel.Jester;
import com.bag_tos.roles.town.Doktor;
import com.bag_tos.roles.town.Jailor;
import com.bag_tos.roles.town.Serif;

import java.util.*;
import java.util.concurrent.*;

public class Game {
    // Oyuncu ve rol yönetimi değişkenleri
    private List<ClientHandler> players;
    private Map<String, Role> roles;
    private List<String> alivePlayers;

    // Aksiyon yönetimi değişkenleri
    private Map<String, Map<ActionType, String>> nightActions;
    private Map<String, String> votes;

    // Zamanlayıcı yönetimi değişkenleri
    private ScheduledExecutorService timer;
    private ScheduledFuture<?> countdownTask;
    private int remainingSeconds;

    // Oyun durumu değişkenleri
    private GamePhase currentPhase;
    private RoomHandler roomHandler;
    private boolean gameOver;

    // Jailor değişkenleri
    private String jailedPlayer = null;
    private String jailorPlayer = null;

    private final Object phaseLock = new Object();
    private volatile boolean phaseTransitionInProgress = false;

    private ScheduledFuture<?> phaseTransitionTask;

    public Game(List<ClientHandler> players) {
        this.players = new ArrayList<>(players);
        this.roles = new HashMap<>();
        this.alivePlayers = new ArrayList<>();
        this.nightActions = new ConcurrentHashMap<>();
        this.votes = new ConcurrentHashMap<>();

        // Eski timer'ı temizle ve yenisini başlat
        if (this.timer != null) {
            this.timer.shutdownNow();
        }
        this.timer = Executors.newScheduledThreadPool(2);

        this.gameOver = false;
        this.currentPhase = GamePhase.LOBBY;

        System.out.println("Yeni Game örneği oluşturuldu, zamanlayıcı başlatıldı");
    }

    public void setRoomHandler(RoomHandler roomHandler) {
        this.roomHandler = roomHandler;
    }

    public void addPlayer(ClientHandler player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public Role getRole(String username) {
        return roles.get(username);
    }

    public List<String> getAlivePlayers() {
        return alivePlayers;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public Map<String, Map<ActionType, String>> getNightActions() {
        return nightActions;
    }

    public void start() {
        // Mafya oyuncularını mafya odasına ekle
        players.stream()
                .filter(p -> getRole(p.getUsername()).getRoleType() == RoleType.MAFYA)
                .forEach(p -> roomHandler.addToRoom("MAFYA", p));

        // Gece fazıyla başla
        startNightPhase();
    }

    public void initializeGame() {
        alivePlayers.clear();
        for (ClientHandler player : players) {
            alivePlayers.add(player.getUsername());
        }

        System.out.println("Oyun başlatılıyor - Hayatta olan oyuncular: " + alivePlayers);
        assignRoles();
    }

    public void assignRoles() {
        List<Role> rolePool = new ArrayList<>();
        int playerCount = players.size();

        // Temel rolleri ekle
        rolePool.add(new Mafya());
        rolePool.add(new Serif());
        rolePool.add(new Doktor());
        rolePool.add(new Jailor());
        rolePool.add(new Jester());

        // Oyuncu sayısına göre ek mafya ekle
        int mafiaCount = Math.max(1, playerCount / GameConfig.MAX_MAFIA_RATIO);
        for (int i = 1; i < mafiaCount; i++) {
            rolePool.add(new Mafya());
        }

        if (playerCount > GameConfig.MAX_PLAYERS) {
            System.out.println("Uyarı: Oyuncu sayısı maksimum sınırı aştı: " + playerCount);
        }

        Collections.shuffle(rolePool);

        while (rolePool.size() > players.size()) {
            rolePool.remove(rolePool.size() - 1);
        }

        for (int i = 0; i < players.size(); i++) {
            ClientHandler player = players.get(i);
            String username = player.getUsername();

            Role role = (i < rolePool.size()) ? rolePool.get(i) : new Jester();
            roles.put(username, role);

            sendRoleAssignment(player, role);
            System.out.println("[DEBUG] Rol Atama: " + username + " -> " + role.getName());
        }
    }

    private void sendRoleAssignment(ClientHandler player, Role role) {
        RoleAssignmentResponse roleResponse = new RoleAssignmentResponse(role.getName());

        Message message = new Message(MessageType.ROLE_ASSIGNMENT);
        message.addData("roleAssignment", roleResponse);
        message.addData("role", role.getName());
        message.addData("roleType", role.getRoleType().name());

        player.sendJsonMessage(message);
    }

    private void startNightPhase() {
        synchronized (phaseLock) {
            // Halihazırda bir faz geçişi devam ediyorsa çık
            if (phaseTransitionInProgress) {
                System.out.println("UYARI: Halihazırda bir faz geçişi devam ediyor, startNightPhase iptal edildi");
                return;
            }

            // Eğer zaten gece fazındaysak, return
            if (currentPhase == GamePhase.NIGHT) {
                System.out.println("UYARI: Zaten gece fazındayız, startNightPhase iptal edildi");
                return;
            }

            try {
                phaseTransitionInProgress = true;
                System.out.println("===== GECE FAZI BAŞLATILIYOR =====");

                // Zamanlayıcıları temizle
                cancelAllTimers();

                currentPhase = GamePhase.NIGHT;
                remainingSeconds = GameConfig.NIGHT_PHASE_DURATION;
                nightActions.clear();

                // Hapishane odasını kur
                if (jailedPlayer != null && jailorPlayer != null) {
                    roomHandler.createJailRoom(jailorPlayer, jailedPlayer);

                    // Hapsedilen oyuncuya bildirim
                    Message jailMessage = new Message(MessageType.GAME_STATE);
                    jailMessage.addData("event", "PLAYER_JAILED");
                    jailMessage.addData("message", "Bu gece gardiyan tarafından hapsedildiniz!");

                    players.stream()
                            .filter(p -> p.getUsername().equals(jailedPlayer))
                            .findFirst()
                            .ifPresent(p -> p.sendJsonMessage(jailMessage));
                }

                broadcastPhaseChange(GamePhase.NIGHT);

                // Kısa bir bekleme ekleyin ki broadcastPhaseChange tamamlanabilsin
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Oyun durumunu yayınla, tüm oyuncuların güncel bilgilere sahip olmasını sağla
                broadcastGameState();

                // Zamanlayıcıları başlat
                startCountdown();
                scheduleNightActions();
                sendAvailableActions();

            } finally {
                phaseTransitionInProgress = false;
            }
        }
    }
    private void startDayPhase() {
        synchronized (phaseLock) {
            // Halihazırda bir faz geçişi devam ediyorsa çık
            if (phaseTransitionInProgress) {
                System.out.println("UYARI: Halihazırda bir faz geçişi devam ediyor, startDayPhase iptal edildi");
                return;
            }

            // Eğer zaten gündüz fazındaysak, return
            if (currentPhase == GamePhase.DAY) {
                System.out.println("UYARI: Zaten gündüz fazındayız, startDayPhase iptal edildi");
                return;
            }

            try {
                phaseTransitionInProgress = true;
                System.out.println("===== GÜNDÜZ FAZI BAŞLATILIYOR =====");

                // Zamanlayıcıları temizle
                cancelAllTimers();

                currentPhase = GamePhase.DAY;
                remainingSeconds = GameConfig.DAY_PHASE_DURATION;
                votes.clear();

                // Kısa bir bekleme ekleyin
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                broadcastPhaseChange(GamePhase.DAY);

                // Kısa bir bekleme ekleyin ki broadcastPhaseChange tamamlanabilsin
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Oyun durumunu yayınla, tüm oyuncuların güncel bilgilere sahip olmasını sağla
                broadcastGameState();

                // Zamanlayıcıları başlat
                startCountdown();
                scheduleDayActions();
                sendAvailableActions();

            } finally {
                phaseTransitionInProgress = false;
            }
        }
    }
    public Message createGameStateMessage() {
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        for (ClientHandler player : players) {
            String username = player.getUsername();
            boolean alive = alivePlayers.contains(username);

            Role role = roles.get(username);
            String roleName = (role != null) ? role.getName() : "UNKNOWN";

            playerInfoList.add(new PlayerInfo(
                    username,
                    alive,
                    GameConfig.REVEAL_ROLES_ON_DEATH && !alive ? roleName : "UNKNOWN"
            ));
        }

        GameStateResponse gameStateResponse = new GameStateResponse(
                currentPhase.name(),
                remainingSeconds,
                playerInfoList
        );

        Message message = new Message(MessageType.GAME_STATE);
        message.addData("gameState", gameStateResponse);
        message.addData("phase", currentPhase.name());
        message.addData("remainingTime", remainingSeconds);
        message.addData("players", playerInfoList);

        return message;
    }

    private void broadcastPhaseChange(GamePhase newPhase) {
        System.out.println("Faz değişimi yayınlanıyor: " + newPhase);

        Message phaseChangeMessage = new Message(MessageType.PHASE_CHANGE);
        phaseChangeMessage.addData("newPhase", newPhase.name());
        phaseChangeMessage.addData("message", newPhase == GamePhase.NIGHT ?
                "Gece fazı başladı!" : "Gündüz fazı başladı!");
        phaseChangeMessage.addData("phaseDuration", newPhase == GamePhase.NIGHT ?
                GameConfig.NIGHT_PHASE_DURATION : GameConfig.DAY_PHASE_DURATION);

        // Kesin zaman bilgisi ekle
        phaseChangeMessage.addData("remainingTime", remainingSeconds);

        // Hapsedilen oyuncu bilgisini ekle
        if (newPhase == GamePhase.NIGHT && jailedPlayer != null) {
            phaseChangeMessage.addData("jailedPlayer", jailedPlayer);
        }


        // Diğer oyun durumu bilgilerini ekle
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        for (ClientHandler player : players) {
            String username = player.getUsername();
            boolean alive = alivePlayers.contains(username);
            String roleName = "UNKNOWN";

            // Ölü oyuncuların rollerini aç
            if (!alive && GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(username);
                if (role != null) {
                    roleName = role.getName();
                }
            }

            playerInfoList.add(new PlayerInfo(username, alive, roleName));
        }
        phaseChangeMessage.addData("players", playerInfoList);

        // Ekstra bir flag ekle ki client tarafı bir sonraki GAME_STATE mesajında faz değişimini yeniden uygulamasın
        phaseChangeMessage.addData("phaseChangeMessage", true);

        // Mesajın ne zaman gönderildiğini belirtmek için timestamp ekle
        phaseChangeMessage.addData("timestamp", System.currentTimeMillis());

        // Tüm oyunculara bildir
        int sentCount = 0;
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                try {
                    player.sendJsonMessage(phaseChangeMessage);
                    sentCount++;
                } catch (Exception e) {
                    System.err.println("Faz değişimi " + player.getUsername() + " oyuncusuna gönderilirken hata: " + e.getMessage());
                }
            }
        }

        System.out.println("Faz değişimi " + sentCount + " oyuncuya gönderildi");
    }
    public void broadcastGameState() {
        try {
            Message gameStateMessage = createGameStateMessage();

            // Faz değişimi olmadığını belirt
            gameStateMessage.addData("skipPhaseUpdate", true);

            // Timestamp ekle
            gameStateMessage.addData("timestamp", System.currentTimeMillis());

            // Log ekle
            System.out.println("Oyun durumu yayınlanıyor: Faz=" + currentPhase + ", Kalan Süre=" + remainingSeconds);

            // Tüm oyunculara bildir
            int sentCount = 0;
            for (ClientHandler player : players) {
                if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                    try {
                        player.sendJsonMessage(gameStateMessage);
                        sentCount++;
                    } catch (Exception e) {
                        System.err.println("Oyun durumu " + player.getUsername() + " oyuncusuna gönderilirken hata: " + e.getMessage());
                    }
                }
            }

            System.out.println("Oyun durumu " + sentCount + " oyuncuya gönderildi");
        } catch (Exception e) {
            System.err.println("Oyun durumu yayınlanırken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void cancelAllTimers() {
        System.out.println("Tüm zamanlayıcılar iptal ediliyor...");

        if (countdownTask != null) {
            countdownTask.cancel(true);
            countdownTask = null;
            System.out.println("Geri sayım zamanlayıcısı iptal edildi");
        }

        if (phaseTransitionTask != null) {
            phaseTransitionTask.cancel(true);
            phaseTransitionTask = null;
            System.out.println("Faz geçiş zamanlayıcısı iptal edildi");
        }

        // Timer'ı temizle ve yeniden oluştur
        if (timer != null) {
            timer.shutdownNow();
            System.out.println("Timer servisi kapatıldı ve yeniden başlatılıyor");
            timer = Executors.newScheduledThreadPool(2);
        }
    }
    private void sendAvailableActions() {
        System.out.println("sendAvailableActions çağrıldı, mevcut faz: " + currentPhase);

        for (ClientHandler player : players) {
            if (!alivePlayers.contains(player.getUsername())) {
                continue; // Ölü oyuncular aksiyon yapamaz
            }

            String username = player.getUsername();
            Role role = roles.get(username);

            if (role == null) {
                continue;
            }

            // Hapsedilen oyuncu gece aksiyon yapamaz
            if (jailedPlayer != null && jailedPlayer.equals(username) &&
                    currentPhase == GamePhase.NIGHT) {
                continue;
            }

            // Jester'ın gece aksiyonu yoktur
            if (role.getRoleType() == RoleType.JESTER &&
                    currentPhase == GamePhase.NIGHT) {
                // Jester için aksiyon yok, boş liste gönder ve mesaj ekle
                Message infoMessage = new Message(MessageType.GAME_STATE);
                infoMessage.addData("message", "Jester olarak gece aksiyonunuz bulunmuyor.");
                player.sendJsonMessage(infoMessage);
                continue;
            }

            // Jailor rolü kontrolü - eğer jailor gündüz kimseyi seçmediyse
            if (role.getRoleType() == RoleType.JAILOR &&
                    currentPhase == GamePhase.NIGHT &&
                    (jailorPlayer == null || !jailorPlayer.equals(username) || jailedPlayer == null)) {
                // Jailor için aksiyon yok, boş liste gönder ve mesaj ekle
                Message infoMessage = new Message(MessageType.GAME_STATE);
                infoMessage.addData("message", "Gündüz fazında kimseyi hapsetmediniz. Bu gece aksiyon gerçekleştiremeyeceksiniz.");
                player.sendJsonMessage(infoMessage);
                continue;
            }

            // Kullanılabilir aksiyonlar mesajı
            Message actionMessage = new Message(MessageType.AVAILABLE_ACTIONS);
            List<String> availableActions = new ArrayList<>();

            // Faza ve role göre aksiyonları belirle
            if (currentPhase == GamePhase.NIGHT) {
                RoleType roleType = role.getRoleType();

                if (roleType == RoleType.MAFYA) {
                    availableActions.add(ActionType.KILL.name());
                } else if (roleType == RoleType.DOKTOR) {
                    availableActions.add(ActionType.HEAL.name());
                } else if (roleType == RoleType.SERIF) {
                    availableActions.add(ActionType.INVESTIGATE.name());
                } else if (roleType == RoleType.JAILOR && jailorPlayer != null &&
                        jailorPlayer.equals(username) && jailedPlayer != null) {
                    availableActions.add(ActionType.EXECUTE.name());
                    System.out.println("Jailor'a EXECUTE aksiyonu eklendi");
                }
            } else if (currentPhase == GamePhase.DAY) {
                availableActions.add(ActionType.VOTE.name());
                if (role.getRoleType() == RoleType.JAILOR) {
                    availableActions.add(ActionType.JAIL.name());
                    System.out.println("Jailor'a JAIL aksiyonu eklendi");
                }
            }

            // Log ekle
            System.out.println(username + " için kullanılabilir aksiyonlar: " + availableActions + ", faz: " + currentPhase);

            // Mevcut aksiyonları ekle
            actionMessage.addData("availableActions", availableActions);

            // Oyuncuya gönder
            player.sendJsonMessage(actionMessage);
        }
    }
    private void startCountdown() {
        System.out.println("startCountdown çağrıldı, kalan süre: " + remainingSeconds + " saniye, faz: " + currentPhase);

        // Mevcut zamanlayıcıyı iptal et
        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(false);
            countdownTask = null;
        }

        countdownTask = timer.scheduleAtFixedRate(() -> {
            try {
                remainingSeconds--;

                // Debug log ekleyin
                if (remainingSeconds % 5 == 0 || remainingSeconds <= 5) {
                    System.out.println("Zamanlayıcı: Kalan süre = " + remainingSeconds + " saniye, faz: " + currentPhase);
                    broadcastGameState();
                }

                if (remainingSeconds <= 0) {
                    System.out.println("Süre doldu: " + currentPhase + " fazı sona eriyor");
                    if (countdownTask != null) {
                        countdownTask.cancel(true);
                        countdownTask = null;
                    }
                }
            } catch (Exception e) {
                System.err.println("Zamanlayıcıda hata: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);

        System.out.println("Zamanlayıcı başlatıldı.");
    }

    private void scheduleNightActions() {
        System.out.println("scheduleNightActions çağrıldı, zamanlayıcı ayarlanıyor...");

        // Önceki zamanlayıcıyı iptal et
        if (phaseTransitionTask != null && !phaseTransitionTask.isDone()) {
            phaseTransitionTask.cancel(false);
        }

        phaseTransitionTask = timer.schedule(() -> {
            System.out.println("Gece fazı sona erdi, gece aksiyonlarını işliyoruz...");
            processNightActions();
            System.out.println("Gece aksiyonları işlendi, oyun durumu: " + (gameOver ? "bitti" : "devam ediyor"));
            if (!gameOver) {
                System.out.println("Gündüz fazına geçiliyor...");
                startDayPhase();
            }
        }, GameConfig.NIGHT_PHASE_DURATION, TimeUnit.SECONDS);
    }

    // Gündüz aksiyonlarını zamanlar
    private void scheduleDayActions() {
        System.out.println("scheduleDayActions çağrıldı, " + GameConfig.DAY_PHASE_DURATION + " saniye sonra faz geçişi planlandı");

        // Önceki zamanlayıcıyı iptal et
        if (phaseTransitionTask != null && !phaseTransitionTask.isDone()) {
            phaseTransitionTask.cancel(false);
            phaseTransitionTask = null;
        }

        phaseTransitionTask = timer.schedule(() -> {
            try {
                System.out.println("Gündüz fazı süresi doldu, oyları işliyoruz...");
                processVotes();
                if (!gameOver) {  // Oyun bitmemişse gece fazına geç
                    System.out.println("Gece fazına geçiliyor...");
                    startNightPhase();
                }
            } catch (Exception e) {
                System.err.println("Gündüz fazı geçişinde hata: " + e.getMessage());
                e.printStackTrace();
            }
        }, GameConfig.DAY_PHASE_DURATION, TimeUnit.SECONDS);

        System.out.println("Faz geçiş zamanlayıcısı başlatıldı");
    }

    // Gece aksiyonunu kaydeder
    public void registerNightAction(String username, ActionType actionType, String target) {
        if (!alivePlayers.contains(username)) {
            return;
        }

        Role role = roles.get(username);
        if (role == null) {
            return;
        }

        RoleType roleType = role.getRoleType();

        if ((actionType == ActionType.KILL && roleType != RoleType.MAFYA) ||
                (actionType == ActionType.HEAL && roleType != RoleType.DOKTOR) ||
                (actionType == ActionType.INVESTIGATE && roleType != RoleType.SERIF)) {
            return;
        }

        if (!GameConfig.ALLOW_SELF_ACTIONS && username.equals(target)) {
            return;
        }

        Map<ActionType, String> playerActions = nightActions.computeIfAbsent(username, k -> new HashMap<>());
        playerActions.put(actionType, target);

        System.out.println("[DEBUG] Gece aksiyonu kaydedildi: " + username + " -> " + actionType + " -> " + target);
    }

    public void registerVote(String username, String target) {
        if (!alivePlayers.contains(username)) {
            return;
        }

        if (!alivePlayers.contains(target)) {
            return;
        }

        if (!GameConfig.ALLOW_SELF_ACTIONS && username.equals(target)) {
            return;
        }

        votes.put(username, target);
        System.out.println("[DEBUG] Oy kaydedildi: " + username + " -> " + target);
        broadcastVoteInformation(username, target);
    }

    private void broadcastVoteInformation(String voter, String target) {
        Message voteMessage = new Message(MessageType.GAME_STATE);
        voteMessage.addData("event", "VOTE_CAST");
        voteMessage.addData("voter", voter);
        voteMessage.addData("target", target);
        voteMessage.addData("message", voter + " oyunu " + target + " için kullandı");

        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(voteMessage);
            }
        }
    }

    private void processNightActions() {
        System.out.println("Gece aksiyonları işleniyor...");

        if (gameOver) {
            System.out.println("Oyun bitmiş, gece aksiyonları işlenmiyor");
            return;
        }

        if (currentPhase != GamePhase.NIGHT) {
            System.out.println("UYARI: Mevcut faz GECE değil, gece aksiyonları işlenmiyor. Mevcut faz: " + currentPhase);
            return;
        }

        try {
            // Hapsedilen oyuncu kontrolü - aksiyonları temizle
            if (jailedPlayer != null) {
                // Hapsedilen oyuncunun bütün aksiyonlarını temizle
                System.out.println("Hapsedilen oyuncunun aksiyonları temizleniyor: " + jailedPlayer);
                nightActions.remove(jailedPlayer);

                // Hapsedilen oyuncuya bildirim gönder
                Message jailNoticeMessage = new Message(MessageType.GAME_STATE);
                jailNoticeMessage.addData("event", "PLAYER_JAILED");
                jailNoticeMessage.addData("message", "Hapsedildiniz, bu gece aksiyon gerçekleştiremezsiniz.");

                players.stream()
                        .filter(p -> p.getUsername().equals(jailedPlayer))
                        .findFirst()
                        .ifPresent(p -> p.sendJsonMessage(jailNoticeMessage));
            }

            Map<ActionType, Map<String, String>> actionTargets = collectActionTargets();

            // Önce gardiyan aksiyonlarını işle
            processJailorActions();
            processMafiaActions(actionTargets.getOrDefault(ActionType.KILL, new HashMap<>()));
            processDoctorActions(actionTargets.getOrDefault(ActionType.HEAL, new HashMap<>()));
            processSheriffActions(actionTargets.getOrDefault(ActionType.INVESTIGATE, new HashMap<>()));

            // Hapishane odasını kapat
            if (jailorPlayer != null) {
                roomHandler.closeJailRoom(jailorPlayer);
            }

            // Hapishane durumunu sıfırla
            resetJailState();
            checkWinConditions();

            // Senkronize bir şekilde faz değişikliğini yap
            synchronized (phaseLock) {
                if (!gameOver && currentPhase == GamePhase.NIGHT) {
                    System.out.println("Gece aksiyonları tamamlandı, gündüz fazına geçiliyor...");
                    startDayPhase();
                } else {
                    System.out.println("Gece aksiyonları tamamlandı, ancak faz değiştirilmiyor. Mevcut durum: "
                            + "gameOver=" + gameOver + ", currentPhase=" + currentPhase);
                }
            }
        } catch (Exception e) {
            System.err.println("Gece aksiyonları işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getJailedPlayer() {
        return jailedPlayer;
    }

    public String getJailorPlayer() {
        return jailorPlayer;
    }

    private void processJailorActions() {
        if (jailedPlayer == null || jailorPlayer == null) {
            System.out.println("Hapsedilen veya gardiyan oyuncu yok, jailor aksiyonları atlanıyor");
            return;
        }

        System.out.println("Jailor aksiyonları işleniyor - Gardiyan: " + jailorPlayer + ", Hapsedilen: " + jailedPlayer);

        // Hapsedilen oyuncu hiçbir aksiyon yapamaz
        nightActions.remove(jailedPlayer);

        // İnfaz kontrolü
        Map<ActionType, String> jailorActions = nightActions.getOrDefault(jailorPlayer, new HashMap<>());
        System.out.println("Gardiyan aksiyonları: " + jailorActions);

        if (jailorActions.containsKey(ActionType.EXECUTE)) {
            System.out.println("Gardiyan infaz aksiyonu gerçekleştiriyor!");

            // Gardiyan infaz aksiyonu aldıysa, hapsedilen oyuncu öldürülür
            killPlayer(jailedPlayer, "EXECUTION");

            // İnfaz mesajı
            Message executionMessage = new Message(MessageType.GAME_STATE);
            executionMessage.addData("event", "JAILOR_EXECUTION");
            executionMessage.addData("target", jailedPlayer);
            executionMessage.addData("message", jailedPlayer + " gardiyan tarafından infaz edildi!");

            // Tüm oyunculara bildir
            for (ClientHandler player : players) {
                if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                    player.sendJsonMessage(executionMessage);
                }
            }
        } else {
            System.out.println("Gardiyan infaz aksiyonu almadı, hapsedilen oyuncu hayatta kaldı");
        }
    }

    public void registerJailAction(String jailor, String target) {
        if (!alivePlayers.contains(jailor) || !alivePlayers.contains(target)) {
            return;
        }

        Role role = roles.get(jailor);
        if (role == null || role.getRoleType() != RoleType.JAILOR) {
            return;
        }

        if (!GameConfig.ALLOW_SELF_ACTIONS && jailor.equals(target)) {
            return;
        }

        jailorPlayer = jailor;
        jailedPlayer = target;

        System.out.println("[DEBUG] Hapis aksiyonu kaydedildi: " + jailor + " -> " + target);

        // Sadece jailor'a bildirim
        Message jailorMessage = new Message(MessageType.ACTION_RESULT);
        ActionResultResponse resultResponse = new ActionResultResponse(
                ActionType.JAIL.name(),
                target,
                "SUCCESS",
                target + " adlı oyuncuyu hapsettiniz."
        );
        jailorMessage.addData("actionResult", resultResponse);

        players.stream()
                .filter(p -> p.getUsername().equals(jailor))
                .findFirst()
                .ifPresent(p -> p.sendJsonMessage(jailorMessage));
    }

    private void killPlayer(String targetUsername, String reason) {
        alivePlayers.remove(targetUsername);

        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(targetUsername))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            // Ölüm mesajı oluştur
            Message deathMessage = new Message(MessageType.ACTION_RESULT);
            ActionResultResponse resultResponse = new ActionResultResponse(
                    reason.equals("EXECUTION") ? ActionType.EXECUTE.name() : ActionType.KILL.name(),
                    targetUsername,
                    reason,
                    reason.equals("EXECUTION") ? "Gardiyan tarafından infaz edildiniz!" : "Öldürüldünüz!"
            );
            deathMessage.addData("actionResult", resultResponse);
            deathMessage.addData("message", reason.equals("EXECUTION") ? "Gardiyan tarafından infaz edildiniz!" : "Öldürüldünüz!");
            deathMessage.addData("alive", false);

            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    deathMessage.addData("role", role.getName());
                }
            }

            // Ölen oyuncuya bildir
            target.sendJsonMessage(deathMessage);

            // Ölüm bildirimini diğer oyunculara gönder
            String eventName = reason.equals("EXECUTION") ? "PLAYER_EXECUTED_BY_JAILOR" : "PLAYER_KILLED";

            Message killMessage = new Message(MessageType.GAME_STATE);
            killMessage.addData("event", eventName);
            killMessage.addData("target", targetUsername);
            killMessage.addData("message", targetUsername + (reason.equals("EXECUTION") ? " gardiyan tarafından infaz edildi!" : " öldürüldü!"));

            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    killMessage.addData("role", role.getName());
                    killMessage.addData("roleRevealed", true);
                }
            }

            // Diğer oyunculara bildir
            for (ClientHandler player : players) {
                if (!player.getUsername().equals(targetUsername) &&
                        (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT)) {
                    player.sendJsonMessage(killMessage);
                }
            }
        }
    }

    private void resetJailState() {
        jailedPlayer = null;
        jailorPlayer = null;
    }

    private Map<ActionType, Map<String, String>> collectActionTargets() {
        Map<ActionType, Map<String, String>> result = new HashMap<>();

        for (Map.Entry<String, Map<ActionType, String>> entry : nightActions.entrySet()) {
            String player = entry.getKey();
            Map<ActionType, String> actions = entry.getValue();

            for (Map.Entry<ActionType, String> action : actions.entrySet()) {
                ActionType actionType = action.getKey();
                String target = action.getValue();

                Map<String, String> actionMap = result.computeIfAbsent(actionType, k -> new HashMap<>());
                actionMap.put(player, target);
            }
        }

        return result;
    }

    private void processMafiaActions(Map<String, String> mafiaActions) {
        if (mafiaActions.isEmpty()) return;

        String mafiaTarget = mafiaActions.values().iterator().next();

        if (isTargetProtected(mafiaTarget)) {
            notifyProtection(mafiaTarget);
        } else {
            killPlayer(mafiaTarget);
        }
    }

    private boolean isTargetProtected(String target) {
        for (Map.Entry<String, Map<ActionType, String>> entry : nightActions.entrySet()) {
            Map<ActionType, String> actions = entry.getValue();
            String healTarget = actions.get(ActionType.HEAL);

            if (healTarget != null && healTarget.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private void notifyProtection(String target) {
        Message protectionMessage = new Message(MessageType.GAME_STATE);
        protectionMessage.addData("event", "PLAYER_PROTECTED");
        protectionMessage.addData("target", target);
        protectionMessage.addData("message", "Doktor birisini korudu!");

        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(protectionMessage);
            }
        }
    }

    private void killPlayer(String targetUsername) {
        alivePlayers.remove(targetUsername);
        handlePlayerKill(targetUsername);
    }

    private void processDoctorActions(Map<String, String> doctorActions) {
        if (doctorActions.isEmpty()) return;

        for (Map.Entry<String, String> entry : doctorActions.entrySet()) {
            String doctor = entry.getKey();
            String target = entry.getValue();

            sendHealingConfirmation(doctor, target);

            if (roles.get(target) != null && roles.get(target).getRoleType() == RoleType.DOKTOR) {
                // Doktorun iyileştirilmesi durumu
            }
        }
    }

    private void sendHealingConfirmation(String doctor, String target) {
        Message resultMessage = new Message(MessageType.ACTION_RESULT);
        ActionResultResponse resultResponse = new ActionResultResponse(
                ActionType.HEAL.name(),
                target,
                "SUCCESS",
                target + " adlı oyuncuyu başarıyla iyileştirdiniz."
        );
        resultMessage.addData("actionResult", resultResponse);

        players.stream()
                .filter(p -> p.getUsername().equals(doctor))
                .findFirst()
                .ifPresent(p -> p.sendJsonMessage(resultMessage));

        if (GameConfig.NOTIFY_HEALED_PLAYERS) {
            Message healedMessage = new Message(MessageType.GAME_STATE);
            healedMessage.addData("event", "PLAYER_HEALED");
            healedMessage.addData("message", "Bu gece doktor tarafından iyileştirildiniz!");

            players.stream()
                    .filter(p -> p.getUsername().equals(target))
                    .findFirst()
                    .ifPresent(p -> p.sendJsonMessage(healedMessage));
        }
    }

    private void processSheriffActions(Map<String, String> sheriffActions) {
        if (sheriffActions.isEmpty()) return;

        for (Map.Entry<String, String> entry : sheriffActions.entrySet()) {
            String sheriff = entry.getKey();
            String target = entry.getValue();

            Role targetRole = roles.get(target);
            boolean isSuspicious = false;

            if (targetRole != null) {
                isSuspicious = targetRole.getRoleType() == RoleType.MAFYA;
            }

            sendInvestigationResult(sheriff, target, isSuspicious);
        }
    }

    private void sendInvestigationResult(String sheriff, String target, boolean isSuspicious) {
        Message resultMessage = new Message(MessageType.ACTION_RESULT);
        ActionResultResponse resultResponse = new ActionResultResponse(
                ActionType.INVESTIGATE.name(),
                target,
                "COMPLETED",
                isSuspicious ? "Bu kişi şüpheli görünüyor!" : "Bu kişi masum görünüyor."
        );
        resultMessage.addData("actionResult", resultResponse);

        players.stream()
                .filter(p -> p.getUsername().equals(sheriff))
                .findFirst()
                .ifPresent(p -> p.sendJsonMessage(resultMessage));
    }

    private void handlePlayerKill(String targetUsername) {
        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(targetUsername))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            Message deathMessage = new Message(MessageType.ACTION_RESULT);
            ActionResultResponse resultResponse = new ActionResultResponse(
                    ActionType.KILL.name(),
                    targetUsername,
                    "KILLED",
                    "Öldürüldünüz!"
            );
            deathMessage.addData("actionResult", resultResponse);
            deathMessage.addData("message", "Öldürüldünüz!");
            deathMessage.addData("alive", false);

            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    deathMessage.addData("role", role.getName());
                }
            }

            target.sendJsonMessage(deathMessage);

            Message killMessage = new Message(MessageType.GAME_STATE);
            killMessage.addData("event", "PLAYER_KILLED");
            killMessage.addData("target", targetUsername);
            killMessage.addData("message", targetUsername + " öldürüldü!");

            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    killMessage.addData("role", role.getName());
                    killMessage.addData("roleRevealed", true);
                }
            }

            for (ClientHandler player : players) {
                if (!player.getUsername().equals(targetUsername) &&
                        (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT)) {
                    player.sendJsonMessage(killMessage);
                }
            }
        }
    }

    private void processVotes() {
        System.out.println("Oylar işleniyor...");

        if (gameOver) {
            System.out.println("Oyun bitmiş, oylar işlenmiyor");
            return;
        }

        if (currentPhase != GamePhase.DAY) {
            System.out.println("UYARI: Mevcut faz GÜNDÜZ değil, oylar işlenmiyor. Mevcut faz: " + currentPhase);
            return;
        }

        try {
            Map<String, Integer> voteCounts = new HashMap<>();

            for (String target : votes.values()) {
                voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
            }

            String executedPlayer = null;
            int maxVotes = 0;

            for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                if (entry.getValue() > maxVotes) {
                    maxVotes = entry.getValue();
                    executedPlayer = entry.getKey();
                }
            }

            if (executedPlayer != null && maxVotes > 0) {
                executePlayer(executedPlayer, new HashMap<>(votes));
            } else {
                notifyNoExecution();
            }

            checkWinConditions();

            // Senkronize bir şekilde faz değişikliğini yap
            synchronized (phaseLock) {
                if (!gameOver && currentPhase == GamePhase.DAY) {
                    System.out.println("Gündüz aksiyonları tamamlandı, gece fazına geçiliyor...");
                    startNightPhase();
                } else {
                    System.out.println("Gündüz aksiyonları tamamlandı, ancak faz değiştirilmiyor. Mevcut durum: "
                            + "gameOver=" + gameOver + ", currentPhase=" + currentPhase);
                }
            }
        } catch (Exception e) {
            System.err.println("Oylar işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void executePlayer(String playerToExecute, Map<String, String> voteDetails) {
        alivePlayers.remove(playerToExecute);

        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(playerToExecute))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            Message executionMessage = new Message(MessageType.ACTION_RESULT);
            ActionResultResponse resultResponse = new ActionResultResponse(
                    ActionType.VOTE.name(),
                    playerToExecute,
                    "EXECUTED",
                    "Asıldınız!"
            );
            executionMessage.addData("actionResult", resultResponse);
            executionMessage.addData("message", "Asıldınız!");
            executionMessage.addData("alive", false);

            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(playerToExecute);
                if (role != null) {
                    executionMessage.addData("role", role.getName());
                }
            }

            target.sendJsonMessage(executionMessage);
        }

        Message executionMessage = new Message(MessageType.GAME_STATE);
        executionMessage.addData("event", "PLAYER_EXECUTED");
        executionMessage.addData("target", playerToExecute);
        executionMessage.addData("message", playerToExecute + " asıldı!");
        executionMessage.addData("votes", voteDetails);

        if (GameConfig.REVEAL_ROLES_ON_DEATH) {
            Role role = roles.get(playerToExecute);
            if (role != null) {
                executionMessage.addData("role", role.getName());
                executionMessage.addData("roleRevealed", true);
            }
        }

        for (ClientHandler player : players) {
            if (!player.getUsername().equals(playerToExecute) &&
                    (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT)) {
                player.sendJsonMessage(executionMessage);
            }
        }
    }

    private void notifyNoExecution() {
        Message noExecutionMessage = new Message(MessageType.GAME_STATE);
        noExecutionMessage.addData("event", "NO_EXECUTION");
        noExecutionMessage.addData("message", "Bugün kimse asılmadı.");

        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(noExecutionMessage);
            }
        }
    }

    private void checkWinConditions() {
        if (gameOver) {
            return;
        }

        int mafyaCount = 0;
        int townCount = 0;

        if (alivePlayers.size() == players.size()) {
            System.out.println("Henüz hiç ölüm yok, oyun devam ediyor.");
            return;
        }

        for (String username : alivePlayers) {
            Role role = roles.get(username);

            if (role == null) {
                continue;
            }

            RoleType roleType = role.getRoleType();

            if (roleType == RoleType.MAFYA) {
                mafyaCount++;
            } else if (roleType == RoleType.DOKTOR || roleType == RoleType.SERIF || roleType == RoleType.JAILOR) {
                townCount++;
            }
        }

        System.out.println("Hayatta kalan sayıları - Mafya: " + mafyaCount + ", Kasaba: " + townCount);

        String winningTeam = null;

        if (mafyaCount == 0) {
            winningTeam = "TOWN";
        } else if (mafyaCount >= townCount && townCount > 0) {
            winningTeam = "MAFIA";
        }

        if (winningTeam != null) {
            gameOver = true;
            endGame(winningTeam);
        }
    }

    private void endGame(String winningTeam) {
        if (countdownTask != null) {
            countdownTask.cancel(true);
        }
        if (phaseTransitionTask != null) {
            phaseTransitionTask.cancel(true);
        }
        timer.shutdownNow();

        String endMessage;
        if ("MAFIA".equals(winningTeam)) {
            endMessage = "OYUN BİTTİ\nMAFYA KAZANDI!";
        } else if ("TOWN".equals(winningTeam)) {
            endMessage = "OYUN BİTTİ\nKÖYLÜLER KAZANDI!";
        } else {
            endMessage = "OYUN BİTTİ\n" + winningTeam + " kazandı!";
        }

        List<Map<String, String>> finalRoles = new ArrayList<>();
        for (ClientHandler player : players) {
            String username = player.getUsername();
            Role role = roles.get(username);

            if (role != null) {
                finalRoles.add(Map.of(
                        "username", username,
                        "role", role.getName(),
                        "roleType", role.getRoleType().name(),
                        "team", roleToTeam(role),
                        "alive", Boolean.toString(alivePlayers.contains(username))
                ));
            }
        }

        Message gameEndMessage = new Message(MessageType.GAME_STATE);
        gameEndMessage.addData("gameOver", true);
        gameEndMessage.addData("winningTeam", winningTeam);
        gameEndMessage.addData("message", endMessage);
        gameEndMessage.addData("finalRoles", finalRoles);

        for (ClientHandler player : players) {
            player.sendJsonMessage(gameEndMessage);
        }
    }

    private String roleToTeam(Role role) {
        if (role == null) return "UNKNOWN";

        RoleType roleType = role.getRoleType();

        if (roleType == RoleType.MAFYA) {
            return "MAFIA";
        } else if (roleType == RoleType.DOKTOR || roleType == RoleType.SERIF || roleType == RoleType.JAILOR) {
            return "TOWN";
        } else {
            return "NEUTRAL";
        }
    }
}