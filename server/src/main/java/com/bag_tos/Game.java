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
import com.bag_tos.roles.town.Serif;

import java.util.*;
import java.util.concurrent.*;

/**
 * Oyun mantığını yöneten ana sınıf
 */
public class Game {
    // Oyuncu ve rol yönetimi
    private List<ClientHandler> players;
    private Map<String, Role> roles;
    private List<String> alivePlayers;

    // Oyun aksiyon yönetimi
    private Map<String, Map<ActionType, String>> nightActions;
    private Map<String, String> votes;

    // Zamanlayıcı yönetimi
    private ScheduledExecutorService timer;
    private ScheduledFuture<?> countdownTask;
    private int remainingSeconds;

    // Oyun durumu
    private GamePhase currentPhase;
    private RoomHandler roomHandler;
    private boolean gameOver;

    /**
     * Yeni bir oyun nesnesi oluşturur
     */
    public Game(List<ClientHandler> players) {
        this.players = new ArrayList<>(players);
        this.roles = new HashMap<>();
        this.alivePlayers = new ArrayList<>();
        this.nightActions = new ConcurrentHashMap<>();
        this.votes = new ConcurrentHashMap<>();
        this.timer = Executors.newScheduledThreadPool(1);
        this.gameOver = false;
        this.currentPhase = GamePhase.LOBBY;
    }

    /**
     * RoomHandler referansını ayarlar
     */
    public void setRoomHandler(RoomHandler roomHandler) {
        this.roomHandler = roomHandler;
    }

    /**
     * Oyuncuyu ekler
     */
    public void addPlayer(ClientHandler player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    /**
     * Oyuncunun rolünü döndürür
     */
    public Role getRole(String username) {
        return roles.get(username);
    }

    /**
     * Mevcut fazı döndürür
     */
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Oyunu başlatır
     */
    public void start() {
        // Oyuncuları mafya odasına ekle (rollere göre)
        players.stream()
                .filter(p -> getRole(p.getUsername()).getRoleType() == RoleType.MAFYA)
                .forEach(p -> roomHandler.addToRoom("MAFYA", p));

        // Gece fazıyla başla
        startNightPhase();
    }

    /**
     * Oyunu başlangıç durumuna getirir
     */
    public void initializeGame() {
        // Tüm oyuncuları hayatta olarak işaretle
        alivePlayers.clear();
        for (ClientHandler player : players) {
            alivePlayers.add(player.getUsername());
        }

        // Rolleri dağıt
        assignRoles();
    }

    /**
     * Oyunculara rolleri dağıtır
     */
    public void assignRoles() {
        List<Role> rolePool = new ArrayList<>();
        int playerCount = players.size();

        // Temel roller
        rolePool.add(new Mafya());
        rolePool.add(new Serif());
        rolePool.add(new Doktor());
        rolePool.add(new Jester());

        // Ek roller (GameConfig'e göre)
        int mafiaCount = Math.max(1, playerCount / GameConfig.MAX_MAFIA_RATIO);
        for (int i = 1; i < mafiaCount; i++) {
            rolePool.add(new Mafya()); // Ek mafya
        }

        // Maksimum oyuncu sayısı kontrolü
        if (playerCount > GameConfig.MAX_PLAYERS) {
            System.out.println("Uyarı: Oyuncu sayısı maksimum sınırı aştı: " + playerCount);
        }

        // Rol listesini karıştır
        Collections.shuffle(rolePool);

        // Oyuncu sayısından fazla rol varsa, fazlalıkları çıkar
        while (rolePool.size() > players.size()) {
            rolePool.remove(rolePool.size() - 1);
        }

        // Rolleri oyunculara dağıt
        for (int i = 0; i < players.size(); i++) {
            ClientHandler player = players.get(i);
            String username = player.getUsername();

            // Rol havuzunda yeterli rol yoksa varsayılan rol ata
            Role role = (i < rolePool.size()) ? rolePool.get(i) : new Jester();

            roles.put(username, role);

            // Rol atamasını bildir
            sendRoleAssignment(player, role);

            System.out.println("[DEBUG] Rol Atama: " + username + " -> " + role.getName());
        }
    }

    /**
     * Oyuncuya rol atamasını bildirir
     */
    private void sendRoleAssignment(ClientHandler player, Role role) {
        // Rol atama yanıtı
        RoleAssignmentResponse roleResponse = new RoleAssignmentResponse(role.getName());

        // Rol atama mesajı
        Message message = new Message(MessageType.ROLE_ASSIGNMENT);
        message.addData("roleAssignment", roleResponse);
        message.addData("role", role.getName());
        message.addData("roleType", role.getRoleType().name());

        // Sadece ilgili oyuncuya gönder
        player.sendJsonMessage(message);
    }

    /**
     * Gece fazını başlatır
     */
    private void startNightPhase() {
        currentPhase = GamePhase.NIGHT;
        remainingSeconds = GameConfig.NIGHT_PHASE_DURATION;
        nightActions.clear();

        // Oyun durumu bildirimi
        broadcastGameState();

        // Zamanlayıcıyı başlat
        startCountdown();

        // Gece aksiyonlarını zamanla
        scheduleNightActions();

        // Oyunculara uygun aksiyonları bildir
        sendAvailableActions();
    }

    /**
     * Gündüz fazını başlatır
     */
    private void startDayPhase() {
        currentPhase = GamePhase.DAY;
        remainingSeconds = GameConfig.DAY_PHASE_DURATION;
        votes.clear();

        // Oyun durumu bildirimi
        broadcastGameState();

        // Zamanlayıcıyı başlat
        startCountdown();

        // Gündüz aksiyonlarını zamanla
        scheduleDayActions();

        // Oyunculara uygun aksiyonları bildir
        sendAvailableActions();
    }

    /**
     * Oyun durumu mesajı oluşturur
     */
    public Message createGameStateMessage() {
        // Oyuncu bilgilerini hazırla
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        for (ClientHandler player : players) {
            String username = player.getUsername();
            boolean alive = alivePlayers.contains(username);

            Role role = roles.get(username);
            String roleName = (role != null) ? role.getName() : "UNKNOWN";

            // Rol bilgisini sadece oyuncunun kendisine göster (veya ölünce göster seçeneği)
            playerInfoList.add(new PlayerInfo(
                    username,
                    alive,
                    GameConfig.REVEAL_ROLES_ON_DEATH && !alive ? roleName : "UNKNOWN"
            ));
        }

        // Oyun durumu yanıtı
        GameStateResponse gameStateResponse = new GameStateResponse(
                currentPhase.name(),
                remainingSeconds,
                playerInfoList
        );

        // Oyun durumu mesajı
        Message message = new Message(MessageType.GAME_STATE);
        message.addData("gameState", gameStateResponse);
        message.addData("phase", currentPhase.name());
        message.addData("remainingTime", remainingSeconds);
        message.addData("players", playerInfoList);

        return message;
    }

    /**
     * Tüm oyunculara mevcut oyun durumunu bildirir
     */
    private void broadcastGameState() {
        // Oyun durumu mesajı oluştur
        Message gameStateMessage = createGameStateMessage();

        // Tüm oyunculara gönder
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(gameStateMessage);
            }
        }
    }

    /**
     * Kullanılabilir aksiyonları oyunculara bildirir
     */
    private void sendAvailableActions() {
        for (ClientHandler player : players) {
            if (!alivePlayers.contains(player.getUsername())) {
                continue; // Ölü oyuncular aksiyon yapamaz
            }

            String username = player.getUsername();
            Role role = roles.get(username);

            if (role == null) {
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
                }
            } else if (currentPhase == GamePhase.DAY) {
                availableActions.add(ActionType.VOTE.name());
            }

            // Mevcut aksiyonları ekle
            actionMessage.addData("availableActions", availableActions);

            // Hedef listesi ekle
            List<String> possibleTargets = new ArrayList<>();
            for (ClientHandler target : players) {
                String targetUsername = target.getUsername();
                // Kendi üzerinde aksiyon yapılabilir mi kontrolü
                if (alivePlayers.contains(targetUsername) &&
                        (GameConfig.ALLOW_SELF_ACTIONS || !targetUsername.equals(username))) {
                    possibleTargets.add(targetUsername);
                }
            }
            actionMessage.addData("possibleTargets", possibleTargets);

            // Oyuncuya gönder
            player.sendJsonMessage(actionMessage);
        }
    }

    /**
     * Geri sayım zamanlayıcısını başlatır
     */
    private void startCountdown() {
        // Önceki zamanlayıcıyı iptal et
        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(true);
        }

        // Yeni zamanlayıcı başlat
        countdownTask = timer.scheduleAtFixedRate(() -> {
            remainingSeconds--;

            // Her 5 saniyede bir durumu bildir
            if (remainingSeconds % 5 == 0 || remainingSeconds <= 5) {
                broadcastGameState();
            }

            if (remainingSeconds <= 0) {
                countdownTask.cancel(true);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Gece aksiyonlarını zamanlar
     */
    private void scheduleNightActions() {
        timer.schedule(() -> {
            processNightActions();
            startDayPhase();
        }, GameConfig.NIGHT_PHASE_DURATION, TimeUnit.SECONDS);
    }

    /**
     * Gündüz aksiyonlarını zamanlar
     */
    private void scheduleDayActions() {
        timer.schedule(() -> {
            processVotes();
            startNightPhase();
        }, GameConfig.DAY_PHASE_DURATION, TimeUnit.SECONDS);
    }

    /**
     * Gece aksiyonunu kaydeder
     */
    public void registerNightAction(String username, ActionType actionType, String target) {
        // Oyuncu hayatta değilse işleme
        if (!alivePlayers.contains(username)) {
            return;
        }

        // Oyuncunun rolü uygun değilse işleme
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

        // Kendi üzerinde aksiyon kontrolü
        if (!GameConfig.ALLOW_SELF_ACTIONS && username.equals(target)) {
            return;
        }

        // Aksiyon kaydı
        Map<ActionType, String> playerActions = nightActions.computeIfAbsent(username, k -> new HashMap<>());
        playerActions.put(actionType, target);

        System.out.println("[DEBUG] Gece aksiyonu kaydedildi: " + username + " -> " + actionType + " -> " + target);
    }

    /**
     * Oylama kaydeder
     */
    public void registerVote(String username, String target) {
        // Oyuncu hayatta değilse işleme
        if (!alivePlayers.contains(username)) {
            return;
        }

        // Hedef hayatta değilse işleme
        if (!alivePlayers.contains(target)) {
            return;
        }

        // Kendi kendine oy kontrolü
        if (!GameConfig.ALLOW_SELF_ACTIONS && username.equals(target)) {
            return;
        }

        // Oylama kaydı
        votes.put(username, target);

        System.out.println("[DEBUG] Oy kaydedildi: " + username + " -> " + target);

        // Oy bilgisini bildir
        broadcastVoteInformation(username, target);
    }

    /**
     * Oy kullanımını tüm oyunculara bildirir
     */
    private void broadcastVoteInformation(String voter, String target) {
        // Oy kullanımı mesajı
        Message voteMessage = new Message(MessageType.GAME_STATE);
        voteMessage.addData("event", "VOTE_CAST");
        voteMessage.addData("voter", voter);
        voteMessage.addData("target", target);
        voteMessage.addData("message", voter + " oyunu " + target + " için kullandı");

        // Tüm oyunculara bildir
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(voteMessage);
            }
        }
    }

    /**
     * Gece aksiyonlarını işler
     */
    private void processNightActions() {
        String mafyaHedef = null;
        String doktorHedef = null;

        // Mafya hedefini bul
        for (Map.Entry<String, Map<ActionType, String>> entry : nightActions.entrySet()) {
            String player = entry.getKey();
            Map<ActionType, String> actions = entry.getValue();

            Role role = roles.get(player);
            if (role != null && role.getRoleType() == RoleType.MAFYA && actions.containsKey(ActionType.KILL)) {
                mafyaHedef = actions.get(ActionType.KILL);
                break; // İlk mafyanın hedefini al
            }
        }

        // Doktor hedefini bul
        for (Map.Entry<String, Map<ActionType, String>> entry : nightActions.entrySet()) {
            String player = entry.getKey();
            Map<ActionType, String> actions = entry.getValue();

            Role role = roles.get(player);
            if (role != null && role.getRoleType() == RoleType.DOKTOR && actions.containsKey(ActionType.HEAL)) {
                doktorHedef = actions.get(ActionType.HEAL);
                break; // İlk doktorun hedefini al
            }
        }

        // Öldürme işlemi
        if (mafyaHedef != null && !mafyaHedef.equals(doktorHedef)) {
            // Hedefi öldür
            alivePlayers.remove(mafyaHedef);

            handlePlayerKill(mafyaHedef);
        } else if (mafyaHedef != null && mafyaHedef.equals(doktorHedef)) {
            // Korunma bildirimi
            Message protectionMessage = new Message(MessageType.GAME_STATE);
            protectionMessage.addData("event", "PLAYER_PROTECTED");
            protectionMessage.addData("target", mafyaHedef);
            protectionMessage.addData("message", "Doktor birisini korudu!");

            // Tüm oyunculara bildir
            for (ClientHandler player : players) {
                if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                    player.sendJsonMessage(protectionMessage);
                }
            }
        }

        // Kazanma durumunu kontrol et
        checkWinConditions();
    }


    private void handlePlayerKill(String targetUsername) {
        // Hedef oyuncuyu bul
        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(targetUsername))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            // Öldürüldü bildirimi
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

            // Öldürülen oyuncuya rolünü göster seçeneği
            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    deathMessage.addData("role", role.getName());
                }
            }

            target.sendJsonMessage(deathMessage);

            // Diğer oyunculara bildir
            Message killMessage = new Message(MessageType.GAME_STATE);
            killMessage.addData("event", "PLAYER_KILLED");
            killMessage.addData("target", targetUsername);
            killMessage.addData("message", targetUsername + " öldürüldü!");

            // Öldürülen oyuncunun rolünü göster seçeneği
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

    /**
     * Oylamaları işler
     */
    private void processVotes() {
        Map<String, Integer> voteCounts = new HashMap<>();

        // Oyları say
        for (String target : votes.values()) {
            voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        }

        // En çok oy alanı bul
        String executedPlayer = null;
        int maxVotes = 0;

        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                executedPlayer = entry.getKey();
            }
        }

        // Asma işlemi veya bildirim
        if (executedPlayer != null && maxVotes > 0) {
            executePlayer(executedPlayer, new HashMap<>(votes)); // Votes'un kopyasını gönder
        } else {
            notifyNoExecution();
        }

        // Kazanma durumunu kontrol et
        checkWinConditions();
    }

    /**
     * Oyuncuyu asma işlemini gerçekleştirir
     */
    private void executePlayer(String playerToExecute, Map<String, String> voteDetails) {
        // Oyuncuyu as
        alivePlayers.remove(playerToExecute);

        // Hedef oyuncuyu bul ve bilgilendir
        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(playerToExecute))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            // Asıldı bildirimi
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

            // Asılan oyuncuya rolünü göster seçeneği
            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(playerToExecute);
                if (role != null) {
                    executionMessage.addData("role", role.getName());
                }
            }

            target.sendJsonMessage(executionMessage);
        }

        // Diğer oyunculara bildir
        Message executionMessage = new Message(MessageType.GAME_STATE);
        executionMessage.addData("event", "PLAYER_EXECUTED");
        executionMessage.addData("target", playerToExecute);
        executionMessage.addData("message", playerToExecute + " asıldı!");
        executionMessage.addData("votes", voteDetails);  // Oy detaylarını da gönder

        // Asılan oyuncunun rolünü göster seçeneği
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

    /**
     * Asılma olmaması durumunda bildirim gönderir
     */
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

    /**
     * Kazanma koşullarını kontrol eder
     */
    private void checkWinConditions() {
        if (gameOver) {
            return; // Oyun zaten bitti
        }

        int mafyaCount = 0;
        int townCount = 0;

        // Hayatta kalan oyuncuların rollerini say
        for (String username : alivePlayers) {
            Role role = roles.get(username);

            if (role == null) {
                continue;
            }

            RoleType roleType = role.getRoleType();

            if (roleType == RoleType.MAFYA) {
                mafyaCount++;
            } else if (roleType == RoleType.DOKTOR || roleType == RoleType.SERIF) {
                townCount++;
            }
            // Jester gibi nötr roller sayılmaz
        }

        // Kazanan takımı belirle
        String winningTeam = null;

        if (mafyaCount == 0) {
            winningTeam = "TOWN";
        } else if (mafyaCount >= townCount) {
            winningTeam = "MAFIA";
        }

        // Kazanan varsa oyunu bitir
        if (winningTeam != null) {
            gameOver = true;
            endGame(winningTeam);
        }
    }

    /**
     * Oyunu bitirir ve sonucu bildirir
     */
    private void endGame(String winningTeam) {
        // Zamanlayıcıyı durdur
        timer.shutdownNow();

        // Kazanan takıma göre mesaj
        String endMessage;
        if ("MAFIA".equals(winningTeam)) {
            endMessage = "OYUN BİTTİ\nMAFYA KAZANDI!";
        } else if ("TOWN".equals(winningTeam)) {
            endMessage = "OYUN BİTTİ\nKÖYLÜLER KAZANDI!";
        } else {
            endMessage = "OYUN BİTTİ\n" + winningTeam + " kazandı!";
        }

        // Rol bilgilerini topla
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

        // Oyun sonu mesajı
        Message gameEndMessage = new Message(MessageType.GAME_STATE);
        gameEndMessage.addData("gameOver", true);
        gameEndMessage.addData("winningTeam", winningTeam);
        gameEndMessage.addData("message", endMessage);
        gameEndMessage.addData("finalRoles", finalRoles);

        // Tüm oyunculara bildir
        for (ClientHandler player : players) {
            player.sendJsonMessage(gameEndMessage);
        }
    }

    /**
     * Rolün hangi takıma ait olduğunu döndürür
     */
    private String roleToTeam(Role role) {
        if (role == null) {
            return "UNKNOWN";
        }

        RoleType roleType = role.getRoleType();

        if (roleType == RoleType.MAFYA) {
            return "MAFIA";
        } else if (roleType == RoleType.DOKTOR || roleType == RoleType.SERIF) {
            return "TOWN";
        } else {
            return "NEUTRAL";
        }
    }
}