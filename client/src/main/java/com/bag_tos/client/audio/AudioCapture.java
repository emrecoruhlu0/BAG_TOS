package com.bag_tos.client.audio;

import com.bag_tos.common.audio.AudioFormat;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mikrofon üzerinden ses kaydı yapma işlemlerini yönetir.
 */
public class AudioCapture implements Runnable {
    private TargetDataLine microphone;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean microphoneActive = new AtomicBoolean(false);
    private Thread captureThread;
    private AudioCaptureListener listener;
    private VoiceActivityDetector voiceDetector;

    /**
     * Ses kaydı sınıfı oluşturur
     */
    public AudioCapture() {
        this.voiceDetector = new VoiceActivityDetector();
    }

    /**
     * Mikrofonu açar ve ses kaydetmeye başlar
     * @throws LineUnavailableException Mikrofon açılamazsa
     */
    public void start() throws LineUnavailableException {
        if (running.get()) {
            return; // Zaten çalışıyor
        }

        // Mikrofon aç
        javax.sound.sampled.AudioFormat format = AudioFormat.getAudioFormat();
        microphone = AudioSystem.getTargetDataLine(format);
        microphone.open(format);
        microphone.start();

        // Kaydı başlat
        running.set(true);
        captureThread = new Thread(this);
        captureThread.setDaemon(true);
        captureThread.start();

        System.out.println("Ses kaydı başlatıldı");
    }

    /**
     * Mikrofonu kapatır ve ses kaydetmeyi durdurur
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return; // Zaten durmuş
        }

        // Thread'i durdur
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }

        // Mikrofonu kapat
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        System.out.println("Ses kaydı durduruldu");
    }

    /**
     * Mikrofonun aktifliğini ayarlar
     * @param active Aktif ise true
     */
    public void setMicrophoneActive(boolean active) {
        microphoneActive.set(active);
    }

    /**
     * Mikrofonun aktif olup olmadığını döndürür
     * @return Aktif ise true
     */
    public boolean isMicrophoneActive() {
        return microphoneActive.get();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[AudioFormat.PACKET_SIZE];

        while (running.get()) {
            try {
                // Mikrofon aktifliğini kontrol et
                if (!microphoneActive.get()) {
                    // Mikrofon pasif, kısa bekle
                    Thread.sleep(100);
                    continue;
                }

                // Ses verisini oku
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // Konuşma var mı kontrol et
                    boolean isSilence = voiceDetector.isSilence(buffer, bytesRead);

                    // Dinleyici varsa bildirimi gönder
                    if (listener != null) {
                        // Kopyasını oluştur
                        byte[] capturedData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, capturedData, 0, bytesRead);

                        listener.onAudioCaptured(capturedData, isSilence);
                    }
                }

            } catch (InterruptedException e) {
                // Thread durduruldu
                break;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("Ses kaydı sırasında hata: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Ses kaydı dinleyicisi ayarlar
     * @param listener Dinleyici
     */
    public void setListener(AudioCaptureListener listener) {
        this.listener = listener;
    }

    /**
     * Ses kaydı dinleyici arayüzü
     */
    public interface AudioCaptureListener {
        void onAudioCaptured(byte[] audioData, boolean isSilence);
    }
}