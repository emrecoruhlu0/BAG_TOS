package com.bag_tos.client.audio;

import com.bag_tos.common.audio.AudioFormat;

public class SimulatedAudioCapture implements AudioCapture.AudioCaptureListener {
    private final String clientId;
    private final VoiceChatManager voiceManager;
    private final int tone; // Her client için farklı ses tonu
    private boolean isActive = false;
    private Thread simulationThread;

    public SimulatedAudioCapture(String clientId, VoiceChatManager voiceManager) {
        this.clientId = clientId;
        this.voiceManager = voiceManager;
        // Kullanıcı adından sayısal bir değer türet (basit hash)
        this.tone = Math.abs(clientId.hashCode() % 4) + 1;

        // Ses yakalama dinleyicisini ayarla
        voiceManager.setSimulatedCaptureListener(this);
    }

    public void startSimulation() {
        if (isActive) return;

        isActive = true;
        simulationThread = new Thread(this::generateAudioLoop);
        simulationThread.setDaemon(true);
        simulationThread.start();

        System.out.println("[SİMÜLASYON] Client " + clientId + " için ses simülasyonu başlatıldı, ton: " + tone);
    }

    public void stopSimulation() {
        isActive = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
            simulationThread = null;
        }
    }

    private void generateAudioLoop() {
        try {
            while (isActive) {
                // Her 500 ms'de bir ses verisi gönder
                if (voiceManager.isMicrophoneActive()) {
                    // Yapay ses verisi oluştur
                    byte[] audioData = generateToneAudio(tone);

                    // Ses verisi işleme metoduna ilet
                    onAudioCaptured(audioData, false);

                    // Konsola bildirim göster
                    System.out.println("[SİMÜLASYON] Client " + clientId +
                            " simüle ses paketi gönderdi: " + audioData.length + " byte");
                }

                Thread.sleep(500); // 500 ms bekle
            }
        } catch (InterruptedException e) {
            // Thread durduruldu, normal durum
        }
    }

    // Farklı tonlarda ses üret
    private byte[] generateToneAudio(int toneType) {
        byte[] audioData = new byte[AudioFormat.PACKET_SIZE];

        // Temel frekans (farklı client'lar için farklı ses tonları)
        double frequency = 440.0 * toneType; // 440Hz, 880Hz, 1320Hz veya 1760Hz

        // Ses sinyali oluştur
        double sampleRate = AudioFormat.SAMPLE_RATE;
        int amplitude = 20000; // Ses seviyesi (0-32767)

        for (int i = 0; i < audioData.length; i += 2) {
            double angle = 2.0 * Math.PI * frequency * (i / 2) / sampleRate;
            short sample = (short)(amplitude * Math.sin(angle));

            // Little Endian formatında 16-bit ses
            audioData[i] = (byte)(sample & 0xFF);
            audioData[i+1] = (byte)((sample >> 8) & 0xFF);
        }

        return audioData;
    }

    @Override
    public void onAudioCaptured(byte[] audioData, boolean isSilence) {
        // VoiceChatManager'a ses verisini ilet
        voiceManager.sendAudioData(audioData, isSilence);
    }
}