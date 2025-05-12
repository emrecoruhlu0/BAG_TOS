package com.bag_tos.common.model;

// common/src/main/java/com/bag_tos/common/model/RoleType.java
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