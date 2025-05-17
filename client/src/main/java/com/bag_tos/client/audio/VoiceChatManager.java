package com.bag_tos.client.audio;

import com.bag_tos.client.model.GameState;
import com.bag_tos.client.network.VoiceNetworkManager;
import com.bag_tos.common.audio.AudioFormat;
import com.bag_tos.common.audio.VoiceCommand;
import com.bag_tos.common.audio.VoicePacket;

import javax.sound.sampled.LineUnavailableException;
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
            return true; // Zaten başlatılmış
        }

        try {
            // Ses bileşenlerini başlat
            audioCapture.start();
            audioPlayback.start();

            // Ağ bağlantısını kur
            boolean connected = networkManager.connect(serverAddress, voicePort, username);
            if (!connected) {
                cleanup();
                return false;
            }

            initialized.set(true);
            System.out.println("Sesli sohbet sistemi başlatıldı");
            return true;

        } catch (LineUnavailableException e) {
            System.err.println("Ses cihazları başlatılamadı: " + e.getMessage());
            cleanup();
            return false;
        }
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

        System.out.println("Sesli sohbet sistemi kapatıldı");
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
     * Mikrofon durumunu ayarlar
     * @param active Aktif ise true
     */
    public void setMicrophoneActive(boolean active) {
        if (microphoneActive.getAndSet(active) != active) {
            // Durum değişti, mikrofonu güncelle
            audioCapture.setMicrophoneActive(active);

            // Sunucuya bildir
            if (active) {
                networkManager.sendUnmuteCommand();
            } else {
                networkManager.sendMuteCommand();
            }

            System.out.println("Mikrofon " + (active ? "açıldı" : "kapatıldı"));
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

            System.out.println("Ses sistemi faz değişimi: " + (isNight ? "GECE" : "GÜNDÜZ"));
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

            System.out.println("Oyuncu " + (isAlive ? "hayatta" : "öldü"));
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

            networkManager.sendVoicePacket(audioData, isSilence);
        }
    }

    // VoiceNetworkManager.VoicePacketListener implementasyonu

    @Override
    public void onVoicePacketReceived(VoicePacket packet) {
        // Ses verisini oynat
        if (initialized.get() && !packet.isSilence()) {
            audioPlayback.queueAudio(packet.getAudioData());
        }
    }

    // VoiceNetworkManager.VoiceCommandListener implementasyonu

    @Override
    public void onVoiceCommandReceived(VoiceCommand command) {
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
        }
    }

    /**
     * Gecikme ölçümü başlatır
     */
    public void measureLatency() {
        if (initialized.get()) {
            networkManager.sendPingCommand();
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}