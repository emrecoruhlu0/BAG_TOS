package com.bag_tos.common.message.response;

public class ActionResultResponse {
    private String action;
    private String target;
    private String result;  // "SUCCESS", "FAILED", "PROTECTED" vb.
    private String message;

    public ActionResultResponse() {
    }

    public ActionResultResponse(String action, String target, String result, String message) {
        this.action = action;
        this.target = target;
        this.result = result;
        this.message = message;
    }

    // Getter ve Setter metodları
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}