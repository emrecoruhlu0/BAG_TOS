package com.bag_tos.common.message.request;


public class VoteRequest {
    private String target;

    public VoteRequest() {
    }

    public VoteRequest(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}