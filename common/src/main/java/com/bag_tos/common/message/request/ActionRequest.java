package com.bag_tos.common.message.request;


public class ActionRequest {
    private String actionType;  // "KILL", "HEAL" gibi aksiyon tipleri
    private String target;      // Hedef oyuncunun kullanıcı adı

    public ActionRequest() {
    }

    public ActionRequest(String actionType, String target) {
        this.actionType = actionType;
        this.target = target;
    }

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