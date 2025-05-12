package com.bag_tos.common.model;

public enum ActionType {
    KILL,      // Öldürme aksiyonu
    HEAL,      // İyileştirme aksiyonu
    INVESTIGATE, // Araştırma aksiyonu
    VOTE,      // Oylama aksiyonu
    JAIL,      // Hapsetme aksiyonu (gündüz)
    EXECUTE,   // İnfaz aksiyonu (gece)
    PASS       // Pas geçme aksiyonu
}