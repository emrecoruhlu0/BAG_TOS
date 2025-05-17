package com.bag_tos.client.audio;

import com.bag_tos.common.audio.AudioFormat;

/**
 * Ses verilerinde konuşma olup olmadığını tespit eder.
 * Sessizlik periyotlarını belirleyerek ağ trafiğini azaltmaya yardımcı olur.
 */
public class VoiceActivityDetector {
    // Daha düşük bir eşik değeri kullan - ses tespitini kolaylaştırmak için
    private static final int SILENCE_THRESHOLD = 300; // 500'den daha düşük

    // Algılama için istatistikler
    private int silenceCount = 0;
    private int speechCount = 0;

    /**
     * Ses verisinin sessizlik olup olmadığını kontrol eder
     * @param audioData Ses verisi
     * @param length Kontrol edilecek uzunluk
     * @return Sessizlik ise true
     */
    public boolean isSilence(byte[] audioData, int length) {
        // Ses verisindeki her sample'ın ortalamasını al
        long sum = 0;
        int sampleSize = AudioFormat.SAMPLE_SIZE_IN_BITS / 8;
        int count = 0;

        for (int i = 0; i < length; i += sampleSize) {
            // 16-bit sample oku (little endian)
            if (i + 1 < length) {
                short sample = (short) ((audioData[i+1] << 8) | (audioData[i] & 0xFF));
                sum += Math.abs(sample);
                count++;
            }
        }

        // Ortalama enerji hesapla
        int average = count > 0 ? (int)(sum / count) : 0;

        // Her 100 algılamada bir istatistikleri logla
        boolean isSilence = average < SILENCE_THRESHOLD;
        if (isSilence) {
            silenceCount++;
        } else {
            speechCount++;
        }

        if ((silenceCount + speechCount) % 100 == 0) {
            System.out.println("[SES-ALGI] Sessizlik algılama istatistikleri - Sessizlik: " + silenceCount +
                    ", Konuşma: " + speechCount + ", Oran: " +
                    String.format("%.2f", (double)silenceCount / (silenceCount + speechCount)) +
                    ", Son ortalama: " + average);
        }

        return isSilence;
    }
}