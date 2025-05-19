package com.bag_tos.common.model;

public enum RoleType {
    MAFYA("Mafya"),
    SERIF("Şerif"),
    DOKTOR("Doktor"),
    JESTER("Jester"),
    JAILOR("Gardiyan"); // Yeni rol eklendi

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}