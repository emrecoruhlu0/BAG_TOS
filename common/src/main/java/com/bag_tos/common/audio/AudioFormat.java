package com.bag_tos.common.audio;

/**
 * Ses verisi için ortak format ayarlarını tanımlar.
 * Client ve server tarafında aynı ses formatı kullanılmasını sağlar.
 */
public class AudioFormat {
    // Ses kaydı ve çalma için sabit ayarlar
    public static final float SAMPLE_RATE = 8000.0F;    // 8kHz - konuşma için yeterli
    public static final int SAMPLE_SIZE_IN_BITS = 16;   // 16-bit
    public static final int CHANNELS = 1;               // Mono ses
    public static final boolean SIGNED = true;          // İmzalı örnekler
    public static final boolean BIG_ENDIAN = false;     // Little endian byte sırası

    // Buffer boyutları
    public static final int BUFFER_SIZE = 1024;         // Ses verisi buffer boyutu
    public static final int PACKET_SIZE = 512;          // Ağ üzerinden iletilecek paket boyutu

    // Konuşma algılama için eşik değeri
    public static final int SILENCE_THRESHOLD = 1000;   // Bu değerin altındaki sesler sessizlik olarak değerlendirilir

    // UDP port ayarları
    public static final int DEFAULT_VOICE_PORT = 50005; // Varsayılan ses portu

    /**
     * Java Sound API'si için javax.sound.sampled.AudioFormat nesnesi oluşturur
     * @return Ses kaydı/çalma için kullanılacak audio format
     */
    public static javax.sound.sampled.AudioFormat getAudioFormat() {
        return new javax.sound.sampled.AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN
        );
    }

    /**
     * Ses veri boyutunu hesaplar (milisaniye cinsinden)
     * @param bytes Ses verisi (byte array)
     * @return Milisaniye cinsinden süre
     */
    public static int calculateDuration(byte[] bytes) {
        // Her örnek için kaç byte kullanılıyor
        int bytesPerSample = SAMPLE_SIZE_IN_BITS / 8;

        // Toplam örnek sayısı
        int sampleCount = bytes.length / bytesPerSample;

        // Süre (ms) = (örnek sayısı / örnekleme hızı) * 1000
        return (int) ((sampleCount / SAMPLE_RATE) * 1000);
    }
}