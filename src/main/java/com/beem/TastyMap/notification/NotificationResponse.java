package com.beem.TastyMap.notification;

public class NotificationResponse {
    private Status status;
    private boolean isUsed;

    public NotificationResponse(Status status, boolean isUsed) {
        this.status = status;
        this.isUsed = isUsed;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }
}
