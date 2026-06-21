package com.beem.TastyMap.event.model;


import java.util.UUID;

public class FcmNotificationEvent {
    private final Long userId;
    private final Long notificationId;

    public FcmNotificationEvent(Long userId, Long notificationId) {
        this.userId = userId;
        this.notificationId = notificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getNotificationId() {
        return notificationId;
    }
}