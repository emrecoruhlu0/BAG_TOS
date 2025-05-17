package com.bag_tos.client.audio;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.VoiceNetworkManager;
import com.bag_tos.common.audio.AudioFormat;
import com.bag_tos.common.audio.VoiceCommand;
import com.bag_tos.common.audio.VoicePacket;

import javax.sound.sampled.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sesli sohbet sistemini yöneten ana sınıf.
 * Ses kaydı, çalma ve ağ iletişimini koordine eder.
 */
public class VoiceChatManager implements AudioCapture.AudioCaptureListener,
        VoiceNetworkManager.VoicePacketListener,
        VoiceNetworkManager.VoiceCommandListener {

    private final VoiceNetworkManager networkManager;
    private final AudioCapture audioCapture;
    private final AudioPlayback audioPlayback;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    // Durum değişkenleri
    private final AtomicBoolean microphoneActive = new AtomicBoolean(false);
    private final AtomicBoolean nightPhase = new AtomicBoolean(false);
    private final AtomicBoolean playerAlive = new AtomicBoolean(true);

    /**
     * Sesli sohbet yöneticisi oluşturur
     */
    public VoiceChatManager() {
        this.networkManager = new VoiceNetworkManager();
        this.audioCapture = new AudioCapture();
        this.audioPlayback = new AudioPlayback();

        // Dinleyicileri ayarla
        this.audioCapture.setListener(this);
        this.networkManager.setPacketListener(this);
        this.networkManager.setCommandListener(this);
    }

    /**
     * Sesli sohbet sistemini başlatır
     * @param serverAddress Sunucu adresi
     * @param voicePort Ses portu
     * @param username Kullanıcı adı
     * @return Başlatma başarılı ise true
     */
    public boolean initialize(String serverAddress, int voicePort, String username) {
        if (initialized.get()) {
            System.out.println("[SES] Sesli sohbet sistemi zaten başlatılmış.");
            return true;
        }

        System.out.println("\n[SES] ===== SESLİ SOHBET SİSTEMİ BAŞLATILIYOR =====");
        System.out.println("[SES] Sunucu: " + serverAddress + ", Port: " + voicePort + ", Kullanıcı: " + username);

        try {
            // Mikrofon erişim izni kontrolü
            System.out.println("[SES] Mikrofon erişilebilirliği kontrol ediliyor...");
            boolean microphoneAvailable = checkMicrophoneAccess();
            if (!microphoneAvailable) {
                System.err.println("[SES] Mikrofon erişilemez veya izin yok!");
                return false;
            }
            System.out.println("[SES] Mikrofona erişim sağlandı.");

            // Hoparlör erişim izni kontrolü
            System.out.println("[SES] Hoparlör erişilebilirliği kontrol ediliyor...");
            boolean speakersAvailable = checkSpeakersAccess();
            if (!speakersAvailable) {
                System.err.println("[SES] Hoparlörlere erişilemez veya izin yok!");
                return false;
            }
            System.out.println("[SES] Hoparlörlere erişim sağlandı.");

            // Ses bileşenlerini başlat
            try {
                System.out.println("[SES] Ses kaydı başlatılıyor...");
                audioCapture.start();
                System.out.println("[SES] Ses kaydı başarıyla başlatıldı.");
            } catch (LineUnavailableException e) {
                System.err.println("[SES] Mikrofon başlatılamadı: " + e.getMessage());
                e.printStackTrace();
                cleanup();
                return false;
            }

            try {
                System.out.println("[SES] Ses çalma başlatılıyor...");
                audioPlayback.start();
                System.out.println("[SES] Ses çalma başarıyla başlatıldı.");
            } catch (LineUnavailableException e) {
                System.err.println("[SES] Hoparlörler başlatılamadı: " + e.getMessage());
                e.printStackTrace();
                cleanup();
                return false;
            }

            // Ağ bağlantısını kur
            System.out.println("[SES] UDP ağ bağlantısı kuruluyor...");
            boolean connected = networkManager.connect(serverAddress, voicePort, username);
            if (!connected) {
                System.err.println("[SES] UDP ağ bağlantısı kurulamadı: " + serverAddress + ":" + voicePort);
                cleanup();
                return false;
            }
            System.out.println("[SES] UDP ağ bağlantısı başarıyla kuruldu.");

            initialized.set(true);
            System.out.println("[SES] Sesli sohbet sistemi başarıyla başlatıldı!");

            // Başlangıç sesli bildirim paketi gönder
            networkManager.sendJoinCommand("LOBBY");
            System.out.println("[SES] LOBBY ses odasına katılım bildirimi gönderildi.");

            return true;

        } catch (Exception e) {
            System.err.println("[SES] Sesli sohbet sistemi başlatılırken beklenmeyen hata: " + e.getMessage());
            e.printStackTrace();
            cleanup();
            return false;
        }
    }

    /**
     * Mikrofon erişim izni kontrolü
     */
    private boolean checkMicrophoneAccess() {
        try {
            // Geçici bir TargetDataLine oluştur ve hemen kapat
            javax.sound.sampled.AudioFormat format = AudioFormat.getAudioFormat();
            TargetDataLine line = AudioSystem.getTargetDataLine(format);

            // Sadece format kontrolü yap, gerçekten açma
            if (AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, format))) {
                // Gerçekte açmayı dene
                line.open(format, AudioFormat.BUFFER_SIZE);
                line.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[SES] Mikrofon erişim kontrolü başarısız: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hoparlör erişim izni kontrolü
     */
    private boolean checkSpeakersAccess() {
        try {
            // Geçici bir SourceDataLine oluştur ve hemen kapat
            javax.sound.sampled.AudioFormat format = AudioFormat.getAudioFormat();
            SourceDataLine line = AudioSystem.getSourceDataLine(format);

            // Sadece format kontrolü yap, gerçekten açma
            if (AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format))) {
                // Gerçekte açmayı dene
                line.open(format, AudioFormat.BUFFER_SIZE);
                line.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[SES] Hoparlör erişim kontrolü başarısız: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kaynakları temizler
     */
    private void cleanup() {
        audioCapture.stop();
        audioPlayback.stop();
        networkManager.disconnect();
    }

    /**
     * Sesli sohbet sistemini kapatır
     */
    public void shutdown() {
        if (!initialized.getAndSet(false)) {
            return; // Zaten kapalı
        }

        // Mikrofonu kapat
        setMicrophoneActive(false);

        // Ağ bağlantısını kapat
        networkManager.disconnect();

        // Ses bileşenlerini kapat
        audioCapture.stop();
        audioPlayback.stop();

        System.out.println("[SES] Sesli sohbet sistemi kapatıldı");
    }

    /**
     * Mikrofon durumunu ayarlar
     * @param active Aktif ise true
     */
    public void setMicrophoneActive(boolean active) {
        if (microphoneActive.getAndSet(active) != active) {
            // Durum değişti, mikrofonu güncelle
            audioCapture.setMicrophoneActive(active);

            // Sunucuya bildir
            if (active) {
                System.out.println("[SES] Mikrofon açıldı, UNMUTE bildirimi gönderiliyor");
                networkManager.sendUnmuteCommand();
            } else {
                System.out.println("[SES] Mikrofon kapatıldı, MUTE bildirimi gönderiliyor");
                networkManager.sendMuteCommand();
            }

            System.out.println("[SES] Mikrofon " + (active ? "açıldı" : "kapatıldı"));
        }
    }

    /**
     * Mikrofonun aktif olup olmadığını döndürür
     * @return Aktif ise true
     */
    public boolean isMicrophoneActive() {
        return microphoneActive.get();
    }

    /**
     * Gece fazı durumunu ayarlar
     * @param isNight Gece fazı ise true
     */
    public void setNightPhase(boolean isNight) {
        if (nightPhase.getAndSet(isNight) != isNight) {
            // Durum değişti

            // Gece fazında mikrofonun kapatılması gerekir
            if (isNight && microphoneActive.get()) {
                setMicrophoneActive(false);
            }

            System.out.println("[SES] Ses sistemi faz değişimi: " + (isNight ? "GECE" : "GÜNDÜZ"));
        }
    }

    /**
     * Oyuncunun hayatta olma durumunu ayarlar
     * @param isAlive Hayatta ise true
     */
    public void setPlayerAlive(boolean isAlive) {
        if (playerAlive.getAndSet(isAlive) != isAlive) {
            // Durum değişti

            // Ölü oyuncunun mikrofonunun kapatılması gerekir
            if (!isAlive && microphoneActive.get()) {
                setMicrophoneActive(false);
            }

            System.out.println("[SES] Oyuncu " + (isAlive ? "hayatta" : "öldü"));
        }
    }

    /**
     * Mikrofon kullanılabilirliğini kontrol eder
     * @return Kullanılabilir ise true
     */
    public boolean canUseMicrophone() {
        // Gece fazında veya ölüyse mikrofon kullanılamaz
        return !nightPhase.get() && playerAlive.get();
    }

    /**
     * GameState ile senkronize eder
     * @param gameState Oyun durumu
     */
    public void synchronizeWithGameState(GameState gameState) {
        // Faz durumunu güncelle
        setNightPhase(gameState.getCurrentPhase() == GameState.Phase.NIGHT);

        // Hayatta olma durumunu güncelle
        setPlayerAlive(gameState.isAlive());
    }

    // AudioCapture.AudioCaptureListener implementasyonu
    @Override
    public void onAudioCaptured(byte[] audioData, boolean isSilence) {
        // Ses verisini sunucuya gönder
        if (initialized.get() && microphoneActive.get()) {
            // Gece fazı veya ölü kontrolü
            if (!canUseMicrophone()) {
                return; // Konuşamaz
            }

            // Ses verisi gönderiyor - detaylı log
            if (!isSilence) {
                System.out.println("[SES] Ses verisi gönderiliyor: " + audioData.length + " byte");
            }

            networkManager.sendVoicePacket(audioData, isSilence);
        }
    }

    // VoiceNetworkManager.VoicePacketListener implementasyonu
    @Override
    public void onVoicePacketReceived(VoicePacket packet) {
        // Ses verisini oynat
        if (initialized.get() && !packet.isSilence()) {
            System.out.println("[SES] Ses paketi alındı: " + packet.getUsername() + " kullanıcısından, " +
                    packet.getAudioData().length + " byte");
            audioPlayback.queueAudio(packet.getAudioData());
        }
    }

    // VoiceNetworkManager.VoiceCommandListener implementasyonu
    @Override
    public void onVoiceCommandReceived(VoiceCommand command) {
        System.out.println("[SES] Ses komutu alındı: " + command.getType() +
                " - Gönderen: " + command.getUsername());

        // Komut tipine göre işle
        switch (command.getType()) {
            case PHASE_CHANGE:
                boolean isNight = "NIGHT".equals(command.getExtraData());
                setNightPhase(isNight);
                break;

            case PLAYER_DIED:
                if (command.getUsername().equals(networkManager.getUsername())) {
                    setPlayerAlive(false);
                }
                break;

            case PONG:
                long pingTime = System.currentTimeMillis() - command.getTimestamp();
                System.out.println("[SES] Ses bağlantısı aktif! Gecikme: " + pingTime + "ms");
                break;
        }
    }

    /**
     * Gecikme ölçümü başlatır
     */
    public void measureLatency() {
        if (initialized.get()) {
            System.out.println("[SES] Ses bağlantısı testi başlatılıyor...");
            networkManager.sendPingCommand();
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Ses bağlantısını test eder (düzenli olarak çağrılmalı)
     */
    public void checkVoiceConnection() {
        if (initialized.get()) {
            // Her 10 saniyede bir ping gönder
            networkManager.sendPingCommand();
        }
    }
}