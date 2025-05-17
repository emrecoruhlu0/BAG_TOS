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
            System.out.println("[KAYIT] Ses kaydı zaten çalışıyor, tekrar başlatılmadı.");
            return; // Zaten çalışıyor
        }

        try {
            // Mikrofon aç
            javax.sound.sampled.AudioFormat format = AudioFormat.getAudioFormat();

            System.out.println("[KAYIT] Mikrofon açılıyor: " + format);
            System.out.println("[KAYIT] Örnek oranı: " + format.getSampleRate() + "Hz");
            System.out.println("[KAYIT] Örnek boyutu: " + format.getSampleSizeInBits() + " bit");
            System.out.println("[KAYIT] Kanal sayısı: " + format.getChannels());

            microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            // Kaydı başlat
            running.set(true);
            captureThread = new Thread(this);
            captureThread.setDaemon(true);
            captureThread.setName("AudioCaptureThread");
            captureThread.start();

            System.out.println("[KAYIT] Ses kaydı başlatıldı");
        } catch (LineUnavailableException e) {
            System.err.println("[KAYIT] Mikrofon açılamadı: " + e.getMessage());
            running.set(false);
            throw e;
        }
    }

    /**
     * Mikrofonu kapatır ve ses kaydetmeyi durdurur
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            System.out.println("[KAYIT] Ses kaydı zaten durdurulmuş.");
            return; // Zaten durmuş
        }

        // Thread'i durdur
        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join(1000); // En fazla 1 saniye bekle
            } catch (InterruptedException e) {
                // Yok sayılabilir
            }
            captureThread = null;
        }

        // Mikrofonu kapat
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        System.out.println("[KAYIT] Ses kaydı durduruldu");
    }

    /**
     * Mikrofonun aktifliğini ayarlar
     * @param active Aktif ise true
     */
    public void setMicrophoneActive(boolean active) {
        boolean changed = microphoneActive.getAndSet(active) != active;
        if (changed) {
            System.out.println("[KAYIT] Mikrofon " + (active ? "etkinleştirildi" : "devre dışı bırakıldı"));
        }
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
        int captureCount = 0;
        long startTime = System.currentTimeMillis();

        System.out.println("[KAYIT] Ses kayıt döngüsü başladı");

        while (running.get()) {
            try {
                // Mikrofon aktifliğini kontrol et
                if (!microphoneActive.get()) {
                    // Mikrofon pasif, kısa bekle
                    Thread.sleep(100);
                    continue;
                }

                // Ses verisini oku
                int bytesRead = 0;
                if (microphone != null && microphone.isOpen()) {
                    bytesRead = microphone.read(buffer, 0, buffer.length);
                } else {
                    System.err.println("[KAYIT] Mikrofonun açık olmadığı tespit edildi, bekleniyor...");
                    Thread.sleep(500);
                    continue;
                }

                if (bytesRead > 0) {
                    captureCount++;

                    // Her 100 kayıtta bir log
                    if (captureCount % 100 == 0) {
                        long currentTime = System.currentTimeMillis();
                        double elapsedSec = (currentTime - startTime) / 1000.0;
                        double captureRate = captureCount / elapsedSec;
                        System.out.println("[KAYIT] " + captureCount + " ses paketi kaydedildi, " +
                                String.format("%.2f", captureRate) + " paket/saniye");
                    }

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
                    System.err.println("[KAYIT] Ses kaydı sırasında hata: " + e.getMessage());
                    e.printStackTrace();

                    // Kısa bir beklemeden sonra devam et
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }

        System.out.println("[KAYIT] Ses kayıt döngüsü sona erdi, toplam " + captureCount + " paket kaydedildi");
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