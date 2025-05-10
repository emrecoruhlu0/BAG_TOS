package com.bag_tos.client;

/**
 * JAR dosyası içinden JavaFX uygulamasını başlatmak için
 * kullanılan yardımcı sınıf.
 * Maven Shade plugin ile paketleme için gerekli.
 */
public class ClientLauncher {
    /**
     * Ana metot
     *
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        // ClientApplication'ı başlat
        ClientApplication.main(args);
    }
}