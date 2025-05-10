package com.bag_tos.common.model;

public enum RoleType {
    MAFYA("Mafya"),
    SERIF("Şerif"),
    DOKTOR("Doktor"),
    JESTER("Jester");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}