package com.bag_tos.common.message.request;

/**
 * Oylama için istek sınıfı
 */
public class VoteRequest {
    private String target;  // Oylanacak oyuncunun kullanıcı adı

    // Boş constructor
    public VoteRequest() {
    }

    // Constructor
    public VoteRequest(String target) {
        this.target = target;
    }

    // Getter ve Setter metodları
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}