package com.bag_tos.common.message.response;

public class ErrorResponse {
    private String code;
    private String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // Getter ve Setter metodları
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}