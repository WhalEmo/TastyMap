package com.beem.TastyMap.user.account.event;

import java.time.LocalDateTime;

public class UserLifecycleEvent {
    private Long userId;
    private UserLifecycleEventType eventType;
    private LocalDateTime timestamp;

    public UserLifecycleEvent() {}

    public UserLifecycleEvent(Long userId, UserLifecycleEventType eventType, LocalDateTime timestamp) {
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public UserLifecycleEventType getEventType() { return eventType; }
    public void setEventType(UserLifecycleEventType eventType) { this.eventType = eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
