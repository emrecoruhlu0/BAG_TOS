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
            return; // Zaten çalışıyor
        }

        // Hoparlör aç
        javax.sound.sampled.AudioFormat format = AudioFormat.getAudioFormat();
        speakers = AudioSystem.getSourceDataLine(format);
        speakers.open(format);
        speakers.start();

        // Çalmayı başlat
        running.set(true);
        playbackThread = new Thread(this);
        playbackThread.setDaemon(true);
        playbackThread.start();

        System.out.println("Ses çalma başlatıldı");
    }

    /**
     * Hoparlörleri kapatır ve ses çalmayı durdurur
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return; // Zaten durmuş
        }

        // Thread'i durdur
        if (playbackThread != null) {
            playbackThread.interrupt();
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
        audioQueue.clear();

        System.out.println("Ses çalma durduruldu");
    }

    /**
     * Ses verisini kuyruğa ekler
     * @param audioData Ses verisi
     */
    public void queueAudio(byte[] audioData) {
        if (running.get() && !audioQueue.offer(audioData)) {
            System.err.println("Ses kuyruğu dolu, paket atıldı");
        }
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                // Kuyruktan veri al
                byte[] audioData = audioQueue.poll(100, TimeUnit.MILLISECONDS);

                if (audioData != null) {
                    // Ses verisini çal
                    speakers.write(audioData, 0, audioData.length);
                }

            } catch (InterruptedException e) {
                // Thread durduruldu
                break;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("Ses çalma sırasında hata: " + e.getMessage());
                }
            }
        }
    }
}