package com.bag_tos.common.message.request;

public class ReadyRequest {
    private boolean ready;

    public ReadyRequest() {
    }

    public ReadyRequest(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}