package com.bag_tos.client.audio;

import com.bag_tos.common.audio.AudioFormat;

/**
 * Ses verilerinde konuşma olup olmadığını tespit eder.
 * Sessizlik periyotlarını belirleyerek ağ trafiğini azaltmaya yardımcı olur.
 */
public class VoiceActivityDetector {
    private static final int SILENCE_THRESHOLD = AudioFormat.SILENCE_THRESHOLD;

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

        // Eşik değeri ile karşılaştır
        return average < SILENCE_THRESHOLD;
    }
}