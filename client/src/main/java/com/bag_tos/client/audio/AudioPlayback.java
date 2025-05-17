package com.bag_tos.client.audio;

import com.bag_tos.common.audio.AudioFormat;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hoparlör üzerinden ses çalma işlemlerini yönetir.
 */
public class AudioPlayback implements Runnable {
    private SourceDataLine speakers;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread playbackThread;

    // Ses verileri için kuyruk
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();

    /**
     * Ses çalma sınıfı oluşturur
     */
    public AudioPlayback() {
    }

    /**
     * Hoparlörleri açar ve ses çalmaya başlar
     * @throws LineUnavailableException Hoparlörler açılamazsa
     */
    public void start() throws LineUnavailableException {
        if (running.get()) {
            System.out.println("[ÇALMA] Ses çalma zaten çalışıyor, tekrar başlatılmadı.");
            return; // Zaten çalışıyor
        }

        try {
            // Hoparlör aç
            javax.sound.sampled.AudioFormat format = AudioFormat.getAudioFormat();

            System.out.println("[ÇALMA] Hoparlör açılıyor: " + format);
            System.out.println("[ÇALMA] Örnek oranı: " + format.getSampleRate() + "Hz");
            System.out.println("[ÇALMA] Örnek boyutu: " + format.getSampleSizeInBits() + " bit");
            System.out.println("[ÇALMA] Kanal sayısı: " + format.getChannels());

            speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            // Çalmayı başlat
            running.set(true);
            playbackThread = new Thread(this);
            playbackThread.setDaemon(true);
            playbackThread.setName("AudioPlaybackThread");
            playbackThread.start();

            System.out.println("[ÇALMA] Ses çalma başlatıldı");
        } catch (LineUnavailableException e) {
            System.err.println("[ÇALMA] Hoparlör açılamadı: " + e.getMessage());
            running.set(false);
            throw e;
        }
    }

    /**
     * Hoparlörleri kapatır ve ses çalmayı durdurur
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            System.out.println("[ÇALMA] Ses çalma zaten durdurulmuş.");
            return; // Zaten durmuş
        }

        // Thread'i durdur
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(1000); // En fazla 1 saniye bekle
            } catch (InterruptedException e) {
                // Yok sayılabilir
            }
            playbackThread = null;
        }

        // Hoparlörleri kapat
        if (speakers != null) {
            speakers.drain();
            speakers.stop();
            speakers.close();
            speakers = null;
        }

        // Kuyruğu temizle
        int discardedPackets = audioQueue.size();
        audioQueue.clear();

        System.out.println("[ÇALMA] Ses çalma durduruldu, " + discardedPackets + " paket atıldı");
    }

    /**
     * Ses verisini kuyruğa ekler
     * @param audioData Ses verisi
     */
    public void queueAudio(byte[] audioData) {
        if (running.get() && !audioQueue.offer(audioData)) {
            System.err.println("[ÇALMA] Ses kuyruğu dolu, paket atıldı");
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[AudioFormat.PACKET_SIZE];
        int playCount = 0;
        long startTime = System.currentTimeMillis();

        System.out.println("[ÇALMA] Ses çalma döngüsü başladı");

        while (running.get()) {
            try {
                // Kuyruktan veri al
                byte[] audioData = audioQueue.poll(100, TimeUnit.MILLISECONDS);

                if (audioData != null) {
                    playCount++;

                    // Her 100 çalmada bir log
                    if (playCount % 100 == 0) {
                        long currentTime = System.currentTimeMillis();
                        double elapsedSec = (currentTime - startTime) / 1000.0;
                        double playRate = playCount / elapsedSec;
                        System.out.println("[ÇALMA] " + playCount + " ses paketi çalındı, " +
                                String.format("%.2f", playRate) + " paket/saniye, kuyrukta " +
                                audioQueue.size() + " paket kaldı");
                    }

                    // Ses verisini çal
                    if (speakers != null && speakers.isOpen()) {
                        speakers.write(audioData, 0, audioData.length);
                    } else {
                        System.err.println("[ÇALMA] Hoparlör açık değil, ses çalınamıyor");
                    }
                }

            } catch (InterruptedException e) {
                // Thread durduruldu
                break;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[ÇALMA] Ses çalma sırasında hata: " + e.getMessage());
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

        System.out.println("[ÇALMA] Ses çalma döngüsü sona erdi, toplam " + playCount + " paket çalındı");
    }
}