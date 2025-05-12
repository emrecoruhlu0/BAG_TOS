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
    // Oyuncu ve rol yönetimi için değişkenler
    private List<ClientHandler> players;
    private Map<String, Role> roles;
    private List<String> alivePlayers;

    // Aksiyon yönetimi için değişkenler
    private Map<String, Map<ActionType, String>> nightActions;
    private Map<String, String> votes;

    // Zamanlayıcı yönetimi için değişkenler
    private ScheduledExecutorService timer;
    private ScheduledFuture<?> countdownTask;
    private int remainingSeconds;

    // Oyun durumu değişkenleri
    private GamePhase currentPhase;
    private RoomHandler roomHandler;
    private boolean gameOver;

    // Sınıf değişkenleri bölümüne ekleyin
    private String jailedPlayer = null;  // Hapsedilen oyuncu
    private String jailorPlayer = null;  // Gardiyan oyuncu

    public Game(List<ClientHandler> players) {
        this.players = new ArrayList<>(players); // Oyuncu listesini kopyalayarak oluştur
        this.roles = new HashMap<>(); // Rol eşleştirmelerini tutacak harita
        this.alivePlayers = new ArrayList<>(); // Hayatta olan oyuncular
        this.nightActions = new ConcurrentHashMap<>(); // Thread-safe aksiyon haritası
        this.votes = new ConcurrentHashMap<>(); // Thread-safe oy haritası
        this.timer = Executors.newScheduledThreadPool(1); // Tek thread'li zamanlayıcı
        this.gameOver = false; // Oyun başlangıçta bitmemiş durumda
        this.currentPhase = GamePhase.LOBBY; // Başlangıçta lobi fazı
    }

    // RoomHandler referansını ayarlar
    public void setRoomHandler(RoomHandler roomHandler) {
        this.roomHandler = roomHandler;
    }

    // Oyuncuyu ekler (eğer zaten yoksa)
    public void addPlayer(ClientHandler player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    // Oyuncunun rolünü döndürür
    public Role getRole(String username) {
        return roles.get(username);
    }

    // Mevcut fazı döndürür
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    // Oyunu başlatır ve rollere göre oda dağıtımı yapar
    public void start() {
        // Mafya oyuncularını mafya odasına ekle
        players.stream()
                .filter(p -> getRole(p.getUsername()).getRoleType() == RoleType.MAFYA)
                .forEach(p -> roomHandler.addToRoom("MAFYA", p));

        // Gece fazıyla başla
        startNightPhase();
    }

    // Oyunu başlangıç durumuna getirir ve tüm oyuncuları hayatta işaretler
    public void initializeGame() {
        // Hayatta olan oyuncuları temizle ve yeniden ekle
        alivePlayers.clear();
        for (ClientHandler player : players) {
            alivePlayers.add(player.getUsername());
        }

        // Rolleri dağıt
        assignRoles();
    }

    // Oyunculara rolleri dağıtır ve bildirir
    public void assignRoles() {
        List<Role> rolePool = new ArrayList<>(); // Rol havuzu
        int playerCount = players.size(); // Toplam oyuncu sayısı

        // Temel rolleri ekle
        rolePool.add(new Mafya());
        rolePool.add(new Serif());
        rolePool.add(new Doktor());
        rolePool.add(new Jester());

        // Oyuncu sayısına göre ek mafya ekle
        int mafiaCount = Math.max(1, playerCount / GameConfig.MAX_MAFIA_RATIO);
        for (int i = 1; i < mafiaCount; i++) {
            rolePool.add(new Mafya());
        }

        // Maksimum oyuncu sayısı kontrolü
        if (playerCount > GameConfig.MAX_PLAYERS) {
            System.out.println("Uyarı: Oyuncu sayısı maksimum sınırı aştı: " + playerCount);
        }

        // Rolleri karıştır
        Collections.shuffle(rolePool);

        // Fazla rolleri çıkar
        while (rolePool.size() > players.size()) {
            rolePool.remove(rolePool.size() - 1);
        }

        // Rolleri oyunculara dağıt
        for (int i = 0; i < players.size(); i++) {
            ClientHandler player = players.get(i);
            String username = player.getUsername();

            // Yeterli rol yoksa Jester ata
            Role role = (i < rolePool.size()) ? rolePool.get(i) : new Jester();
            roles.put(username, role);

            // Rol atamasını bildir
            sendRoleAssignment(player, role);

            System.out.println("[DEBUG] Rol Atama: " + username + " -> " + role.getName());
        }
    }

    // Oyuncuya rol atamasını bildirir
    private void sendRoleAssignment(ClientHandler player, Role role) {
        // Rol atama yanıtı oluştur
        RoleAssignmentResponse roleResponse = new RoleAssignmentResponse(role.getName());

        // Mesaj oluştur
        Message message = new Message(MessageType.ROLE_ASSIGNMENT);
        message.addData("roleAssignment", roleResponse);
        message.addData("role", role.getName());
        message.addData("roleType", role.getRoleType().name());

        // Sadece ilgili oyuncuya gönder
        player.sendJsonMessage(message);
    }

    // Gece fazını başlatır
    private void startNightPhase() {
        currentPhase = GamePhase.NIGHT; // Faz güncelleme
        remainingSeconds = GameConfig.NIGHT_PHASE_DURATION; // Süreyi ayarlama
        nightActions.clear(); // Gece aksiyonlarını temizleme

        broadcastGameState(); // Oyun durumunu bildir
        startCountdown(); // Zamanlayıcıyı başlat
        scheduleNightActions(); // Gece aksiyonlarını zamanla
        sendAvailableActions(); // Aksiyonları bildir
    }

    // Gündüz fazını başlatır
    private void startDayPhase() {
        currentPhase = GamePhase.DAY; // Faz güncelleme
        remainingSeconds = GameConfig.DAY_PHASE_DURATION; // Süreyi ayarlama
        votes.clear(); // Oyları temizleme

        broadcastGameState(); // Oyun durumunu bildir
        startCountdown(); // Zamanlayıcıyı başlat
        scheduleDayActions(); // Gündüz aksiyonlarını zamanla
        sendAvailableActions(); // Aksiyonları bildir
    }

    // Oyun durumu mesajı oluşturur
    public Message createGameStateMessage() {
        // Oyuncu bilgilerini hazırla
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        for (ClientHandler player : players) {
            String username = player.getUsername();
            boolean alive = alivePlayers.contains(username);

            Role role = roles.get(username);
            String roleName = (role != null) ? role.getName() : "UNKNOWN";

            // Rol bilgisini sadece ilgili koşullarda göster
            playerInfoList.add(new PlayerInfo(
                    username,
                    alive,
                    GameConfig.REVEAL_ROLES_ON_DEATH && !alive ? roleName : "UNKNOWN"
            ));
        }

        // Oyun durumu yanıtı oluştur
        GameStateResponse gameStateResponse = new GameStateResponse(
                currentPhase.name(),
                remainingSeconds,
                playerInfoList
        );

        // Mesaj oluştur
        Message message = new Message(MessageType.GAME_STATE);
        message.addData("gameState", gameStateResponse);
        message.addData("phase", currentPhase.name());
        message.addData("remainingTime", remainingSeconds);
        message.addData("players", playerInfoList);

        return message;
    }

    // Tüm oyunculara mevcut oyun durumunu bildirir
    private void broadcastGameState() {
        Message gameStateMessage = createGameStateMessage();

        // Hayatta olan veya izin verilen ölü oyunculara gönder
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(gameStateMessage);
            }
        }
    }

    // Kullanılabilir aksiyonları oyunculara bildirir
    private void sendAvailableActions() {
        for (ClientHandler player : players) {
            // Ölü oyuncular aksiyon yapamaz
            if (!alivePlayers.contains(player.getUsername())) {
                continue;
            }

            String username = player.getUsername();
            Role role = roles.get(username);

            if (role == null) {
                continue;
            }

            // Aksiyon mesajı oluştur
            Message actionMessage = new Message(MessageType.AVAILABLE_ACTIONS);
            List<String> availableActions = new ArrayList<>();

            // Faz ve role göre aksiyon belirleme
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

            actionMessage.addData("availableActions", availableActions);

            // Hedef listesi oluşturma
            List<String> possibleTargets = new ArrayList<>();
            for (ClientHandler target : players) {
                String targetUsername = target.getUsername();
                // Kendi üzerinde aksiyon kontrolü
                if (alivePlayers.contains(targetUsername) &&
                        (GameConfig.ALLOW_SELF_ACTIONS || !targetUsername.equals(username))) {
                    possibleTargets.add(targetUsername);
                }
            }
            actionMessage.addData("possibleTargets", possibleTargets);

            player.sendJsonMessage(actionMessage);
        }
    }

    // Geri sayım zamanlayıcısını başlatır
    private void startCountdown() {
        // Önceki zamanlayıcıyı iptal et
        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(true);
        }

        // Yeni zamanlayıcı başlat
        countdownTask = timer.scheduleAtFixedRate(() -> {
            remainingSeconds--; // Zamanı güncelle

            // Düzenli bildirim
            if (remainingSeconds % 5 == 0 || remainingSeconds <= 5) {
                broadcastGameState();
            }

            // Zaman dolduğunda durdur
            if (remainingSeconds <= 0) {
                countdownTask.cancel(true);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // Gece aksiyonlarını zamanlar
    private void scheduleNightActions() {
        timer.schedule(() -> {
            processNightActions(); // Aksiyonları işle
            startDayPhase(); // Gündüz fazına geç
        }, GameConfig.NIGHT_PHASE_DURATION, TimeUnit.SECONDS);
    }

    // Gündüz aksiyonlarını zamanlar
    private void scheduleDayActions() {
        timer.schedule(() -> {
            processVotes(); // Oyları işle
            startNightPhase(); // Gece fazına geç
        }, GameConfig.DAY_PHASE_DURATION, TimeUnit.SECONDS);
    }

    // Gece aksiyonunu kaydeder
    public void registerNightAction(String username, ActionType actionType, String target) {
        // Oyuncu hayatta değilse işleme
        if (!alivePlayers.contains(username)) {
            return;
        }

        // Oyuncu rolü kontrolü
        Role role = roles.get(username);
        if (role == null) {
            return;
        }

        RoleType roleType = role.getRoleType();

        // Rol-aksiyon uygunluk kontrolü
        if ((actionType == ActionType.KILL && roleType != RoleType.MAFYA) ||
                (actionType == ActionType.HEAL && roleType != RoleType.DOKTOR) ||
                (actionType == ActionType.INVESTIGATE && roleType != RoleType.SERIF)) {
            return;
        }

        // Kendi üzerinde aksiyon kontrolü
        if (!GameConfig.ALLOW_SELF_ACTIONS && username.equals(target)) {
            return;
        }

        // Aksiyonu kaydet
        Map<ActionType, String> playerActions = nightActions.computeIfAbsent(username, k -> new HashMap<>());
        playerActions.put(actionType, target);

        System.out.println("[DEBUG] Gece aksiyonu kaydedildi: " + username + " -> " + actionType + " -> " + target);
    }

    // Oylama kaydeder
    public void registerVote(String username, String target) {
        // Hayatta olma kontrolü
        if (!alivePlayers.contains(username)) {
            return;
        }

        // Hedef hayatta olma kontrolü
        if (!alivePlayers.contains(target)) {
            return;
        }

        // Kendi kendine oy kontrolü
        if (!GameConfig.ALLOW_SELF_ACTIONS && username.equals(target)) {
            return;
        }

        // Oyu kaydet
        votes.put(username, target);

        System.out.println("[DEBUG] Oy kaydedildi: " + username + " -> " + target);

        // Oy bilgisini bildir
        broadcastVoteInformation(username, target);
    }

    // Oy kullanımını tüm oyunculara bildirir
    private void broadcastVoteInformation(String voter, String target) {
        // Oy mesajı oluştur
        Message voteMessage = new Message(MessageType.GAME_STATE);
        voteMessage.addData("event", "VOTE_CAST");
        voteMessage.addData("voter", voter);
        voteMessage.addData("target", target);
        voteMessage.addData("message", voter + " oyunu " + target + " için kullandı");

        // Bildirim gönder
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(voteMessage);
            }
        }
    }

    // Gece aksiyonlarını işler
    private void processNightActions() {
        // Hedefleri belirle
        Map<ActionType, Map<String, String>> actionTargets = collectActionTargets();

        // Gece aksiyonlarını sırasıyla uygula
        processJailorActions(); // Önce gardiyan aksiyonlarını işle (hapisteki oyuncu diğer aksiyonları yapamaz)
        processMafiaActions(actionTargets.getOrDefault(ActionType.KILL, new HashMap<>()));
        processDoctorActions(actionTargets.getOrDefault(ActionType.HEAL, new HashMap<>()));
        processSheriffActions(actionTargets.getOrDefault(ActionType.INVESTIGATE, new HashMap<>()));

        // Hapishane durumunu sıfırla
        resetJailState();

        // Kazanma durumunu kontrol et
        checkWinConditions();
    }

    private void processJailorActions() {
        if (jailedPlayer == null || jailorPlayer == null) return;

        // Hapsedilen oyuncu hiçbir aksiyon yapamaz
        nightActions.remove(jailedPlayer);

        // İnfaz kontrolü
        Map<ActionType, String> jailorActions = nightActions.getOrDefault(jailorPlayer, new HashMap<>());
        if (jailorActions.containsKey(ActionType.EXECUTE)) {
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
        }
    }

    private void killPlayer(String targetUsername, String reason) {
        // Listeden çıkar
        alivePlayers.remove(targetUsername);

        // Hedef oyuncuyu bul
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

            // Rol bilgisi ekle (opsiyonel)
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

            // Rol bilgisi ekle (opsiyonel)
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

    // Aksiyonları tiplere göre gruplandırır
    private Map<ActionType, Map<String, String>> collectActionTargets() {
        Map<ActionType, Map<String, String>> result = new HashMap<>();

        // Her oyuncunun aksiyonlarını döngüyle işle
        for (Map.Entry<String, Map<ActionType, String>> entry : nightActions.entrySet()) {
            String player = entry.getKey();
            Map<ActionType, String> actions = entry.getValue();

            // Her aksiyonu tipine göre grupla
            for (Map.Entry<ActionType, String> action : actions.entrySet()) {
                ActionType actionType = action.getKey();
                String target = action.getValue();

                Map<String, String> actionMap = result.computeIfAbsent(actionType, k -> new HashMap<>());
                actionMap.put(player, target);
            }
        }

        return result;
    }

    // Mafya aksiyonlarını işler
    private void processMafiaActions(Map<String, String> mafiaActions) {
        if (mafiaActions.isEmpty()) return;

        // İlk mafyanın hedefini al
        String mafiaTarget = mafiaActions.values().iterator().next();

        // Koruma kontrolü
        if (isTargetProtected(mafiaTarget)) {
            notifyProtection(mafiaTarget);
        } else {
            // Hedefi öldür
            killPlayer(mafiaTarget);
        }
    }

    // Hedefin doktor tarafından korunup korunmadığını kontrol eder
    private boolean isTargetProtected(String target) {
        // Koruma aksiyonlarını ara
        for (Map.Entry<String, Map<ActionType, String>> entry : nightActions.entrySet()) {
            Map<ActionType, String> actions = entry.getValue();
            String healTarget = actions.get(ActionType.HEAL);

            if (healTarget != null && healTarget.equals(target)) {
                return true;
            }
        }
        return false;
    }

    // Korunma bildirimini gönderir
    private void notifyProtection(String target) {
        // Koruma mesajı oluştur
        Message protectionMessage = new Message(MessageType.GAME_STATE);
        protectionMessage.addData("event", "PLAYER_PROTECTED");
        protectionMessage.addData("target", target);
        protectionMessage.addData("message", "Doktor birisini korudu!");

        // Bildirim gönder
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(protectionMessage);
            }
        }
    }

    // Oyuncuyu öldürür
    private void killPlayer(String targetUsername) {
        // Listeden çıkar
        alivePlayers.remove(targetUsername);
        handlePlayerKill(targetUsername);
    }

    // Doktor aksiyonlarını işler
    private void processDoctorActions(Map<String, String> doctorActions) {
        if (doctorActions.isEmpty()) return;

        // Her doktor aksiyonunu işle
        for (Map.Entry<String, String> entry : doctorActions.entrySet()) {
            String doctor = entry.getKey();
            String target = entry.getValue();

            // Doktora bildirim gönder
            sendHealingConfirmation(doctor, target);

            // Özel durumlar (opsiyonel)
            if (roles.get(target) != null && roles.get(target).getRoleType() == RoleType.DOKTOR) {
                // Doktorun iyileştirilmesi durumu
            }
        }
    }

    // Doktora iyileştirme bildirimi gönderir
    private void sendHealingConfirmation(String doctor, String target) {
        // Sonuç mesajı oluştur
        Message resultMessage = new Message(MessageType.ACTION_RESULT);
        ActionResultResponse resultResponse = new ActionResultResponse(
                ActionType.HEAL.name(),
                target,
                "SUCCESS",
                target + " adlı oyuncuyu başarıyla iyileştirdiniz."
        );
        resultMessage.addData("actionResult", resultResponse);

        // Doktora gönder
        players.stream()
                .filter(p -> p.getUsername().equals(doctor))
                .findFirst()
                .ifPresent(p -> p.sendJsonMessage(resultMessage));

        // İyileştirilen kişiye bildirim (opsiyonel)
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

    // Şerif aksiyonlarını işler
    private void processSheriffActions(Map<String, String> sheriffActions) {
        if (sheriffActions.isEmpty()) return;

        // Her şerifin aksiyonunu işle
        for (Map.Entry<String, String> entry : sheriffActions.entrySet()) {
            String sheriff = entry.getKey();
            String target = entry.getValue();

            // Hedef rol kontrolü
            Role targetRole = roles.get(target);
            boolean isSuspicious = false;

            if (targetRole != null) {
                isSuspicious = targetRole.getRoleType() == RoleType.MAFYA;
            }

            // Sonuç bildir
            sendInvestigationResult(sheriff, target, isSuspicious);
        }
    }

    // Şerife araştırma sonucunu gönderir
    private void sendInvestigationResult(String sheriff, String target, boolean isSuspicious) {
        // Sonuç mesajı oluştur
        Message resultMessage = new Message(MessageType.ACTION_RESULT);
        ActionResultResponse resultResponse = new ActionResultResponse(
                ActionType.INVESTIGATE.name(),
                target,
                "COMPLETED",
                isSuspicious ? "Bu kişi şüpheli görünüyor!" : "Bu kişi masum görünüyor."
        );
        resultMessage.addData("actionResult", resultResponse);

        // Şerife gönder
        players.stream()
                .filter(p -> p.getUsername().equals(sheriff))
                .findFirst()
                .ifPresent(p -> p.sendJsonMessage(resultMessage));
    }

    // Oyuncu ölümünü işler ve bildirir
    private void handlePlayerKill(String targetUsername) {
        // Hedefi bul
        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(targetUsername))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            // Ölüm mesajı oluştur
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

            // Rol bilgisi ekle (opsiyonel)
            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    deathMessage.addData("role", role.getName());
                }
            }

            // Ölene bildir
            target.sendJsonMessage(deathMessage);

            // Diğerlerine bildir
            Message killMessage = new Message(MessageType.GAME_STATE);
            killMessage.addData("event", "PLAYER_KILLED");
            killMessage.addData("target", targetUsername);
            killMessage.addData("message", targetUsername + " öldürüldü!");

            // Rol bilgisi ekle (opsiyonel)
            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(targetUsername);
                if (role != null) {
                    killMessage.addData("role", role.getName());
                    killMessage.addData("roleRevealed", true);
                }
            }

            // Bildirim gönder
            for (ClientHandler player : players) {
                if (!player.getUsername().equals(targetUsername) &&
                        (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT)) {
                    player.sendJsonMessage(killMessage);
                }
            }
        }
    }

    // Oylamaları işler
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
            executePlayer(executedPlayer, new HashMap<>(votes)); // Votes kopyası
        } else {
            notifyNoExecution();
        }

        // Kazanma kontrolü
        checkWinConditions();
    }

    // Oyuncuyu asar ve bildirir
    private void executePlayer(String playerToExecute, Map<String, String> voteDetails) {
        // Listeden çıkar
        alivePlayers.remove(playerToExecute);

        // Hedefi bul
        Optional<ClientHandler> targetOptional = players.stream()
                .filter(p -> p.getUsername().equals(playerToExecute))
                .findFirst();

        if (targetOptional.isPresent()) {
            ClientHandler target = targetOptional.get();
            target.setAlive(false);

            // Asılma mesajı oluştur
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

            // Rol bilgisi ekle (opsiyonel)
            if (GameConfig.REVEAL_ROLES_ON_DEATH) {
                Role role = roles.get(playerToExecute);
                if (role != null) {
                    executionMessage.addData("role", role.getName());
                }
            }

            // Asılana bildir
            target.sendJsonMessage(executionMessage);
        }

        // Diğerlerine bildir
        Message executionMessage = new Message(MessageType.GAME_STATE);
        executionMessage.addData("event", "PLAYER_EXECUTED");
        executionMessage.addData("target", playerToExecute);
        executionMessage.addData("message", playerToExecute + " asıldı!");
        executionMessage.addData("votes", voteDetails);

        // Rol bilgisi ekle (opsiyonel)
        if (GameConfig.REVEAL_ROLES_ON_DEATH) {
            Role role = roles.get(playerToExecute);
            if (role != null) {
                executionMessage.addData("role", role.getName());
                executionMessage.addData("roleRevealed", true);
            }
        }

        // Bildirim gönder
        for (ClientHandler player : players) {
            if (!player.getUsername().equals(playerToExecute) &&
                    (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT)) {
                player.sendJsonMessage(executionMessage);
            }
        }
    }

    // Asma olmadığını bildirir
    private void notifyNoExecution() {
        // Mesaj oluştur
        Message noExecutionMessage = new Message(MessageType.GAME_STATE);
        noExecutionMessage.addData("event", "NO_EXECUTION");
        noExecutionMessage.addData("message", "Bugün kimse asılmadı.");

        // Bildirim gönder
        for (ClientHandler player : players) {
            if (alivePlayers.contains(player.getUsername()) || GameConfig.ALLOW_DEAD_CHAT) {
                player.sendJsonMessage(noExecutionMessage);
            }
        }
    }

    // Kazanma koşullarını kontrol eder
    private void checkWinConditions() {
        if (gameOver) return; // Oyun zaten bittiyse çık

        int mafyaCount = 0;
        int townCount = 0;

        // Hayatta kalan oyuncuların rollerini say
        for (String username : alivePlayers) {
            Role role = roles.get(username);
            if (role == null) continue;

            RoleType roleType = role.getRoleType();

            if (roleType == RoleType.MAFYA) {
                mafyaCount++; // Mafya sayısını artır
            } else if (roleType == RoleType.DOKTOR || roleType == RoleType.SERIF) {
                townCount++; // Kasaba sakini sayısını artır
            }
            // Jester gibi nötr roller sayılmaz
        }

        // Kazanan takımı belirle
        String winningTeam = null;

        if (mafyaCount == 0) {
            winningTeam = "TOWN"; // Mafya kalmadıysa kasaba kazanır
        } else if (mafyaCount >= townCount) {
            winningTeam = "MAFIA"; // Mafya sayısı kasaba sakinlerine eşit veya fazlaysa mafya kazanır
        }

        // Kazanan varsa oyunu bitir
        if (winningTeam != null) {
            gameOver = true;
            endGame(winningTeam);
        }
    }

    // Oyunu bitirir ve sonucu bildirir
    private void endGame(String winningTeam) {
        // Zamanlayıcıyı durdur
        timer.shutdownNow();

        // Kazanan mesajı oluştur
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

        // Oyun sonu mesajı oluştur
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

    // Rolün hangi takıma ait olduğunu döndürür
    private String roleToTeam(Role role) {
        if (role == null) return "UNKNOWN";

        RoleType roleType = role.getRoleType();

        if (roleType == RoleType.MAFYA) {
            return "MAFIA"; // Mafya takımı
        } else if (roleType == RoleType.DOKTOR || roleType == RoleType.SERIF) {
            return "TOWN"; // Kasaba takımı
        } else {
            return "NEUTRAL"; // Tarafsız
        }
    }
}