package com.bag_tos.common.message.request;

/**
 * Oyuncu aksiyonları için istek sınıfı
 */
public class ActionRequest {
    private String actionType;  // "KILL", "HEAL" gibi aksiyon tipleri
    private String target;      // Hedef oyuncunun kullanıcı adı

    // Boş constructor
    public ActionRequest() {
    }

    // Constructor
    public ActionRequest(String actionType, String target) {
        this.actionType = actionType;
        this.target = target;
    }

    // Getter ve Setter metodları
    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}