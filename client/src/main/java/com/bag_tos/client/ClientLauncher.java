package com.bag_tos.client;

/**
 * JAR dosyası içinden JavaFX uygulamasını başlatmak için
 * kullanılan yardımcı sınıf.
 * Maven Shade plugin ile paketleme için gerekli.
 */
public class ClientLauncher {
    public static void main(String[] args) {
        ClientApplication.main(args);
    }
}