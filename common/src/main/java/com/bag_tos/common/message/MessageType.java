package com.bag_tos.common.message;

/**
 * İstemci ve sunucu arasında gönderilen mesaj tiplerini tanımlar
 */
public enum MessageType {
    // İstemciden sunucuya gidecek komutlar
    READY,              // Oyuncunun hazır olduğunu bildirir
    START_GAME,         // Oyunu başlatma isteği
    ACTION,             // Oyun içi aksiyon (öldürme, iyileştirme vb.)
    VOTE,               // Oylama
    CHAT,               // Sohbet mesajı

    // Sunucudan istemciye gidecek bildirimler
    GAME_STATE,         // Genel oyun durumu güncellemesi
    PLAYER_JOIN,        // Yeni oyuncu katılımı
    PLAYER_LEAVE,       // Oyuncu ayrılması
    ROLE_ASSIGNMENT,    // Rol atama
    AVAILABLE_ACTIONS,  // Kullanılabilir aksiyonlar listesi
    ACTION_RESULT,      // Aksiyon sonucu
    CHAT_MESSAGE,       // Sohbet mesajı
    ERROR              // Hata bildirimi
}