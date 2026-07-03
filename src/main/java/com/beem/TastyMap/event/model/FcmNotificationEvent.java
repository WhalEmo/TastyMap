package com.beem.TastyMap.event.model;


import java.util.UUID;

public class FcmNotificationEvent {
    private final Long userId;
    private final Long notificationId;
    private final String city;

    public FcmNotificationEvent(Long userId, Long notificationId, String city) {
        this.userId = userId;
        this.notificationId = notificationId;
        this.city = city;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public String getCity() {
        return city;
    }
}