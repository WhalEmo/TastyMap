package com.beem.TastyMap.event.model;

public class SecurityAlertEvent {
        private final Long userId;
        private final String email;
        private final String deviceId;
        private final String userAgent;
        private final String ip;
        private final String token;
        private final boolean isTrusted;

    public SecurityAlertEvent(Long userId, String email, String deviceId, String userAgent, String ip, String token, boolean isTrusted) {
        this.userId = userId;
        this.email = email;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.ip = ip;
        this.token = token;
        this.isTrusted = isTrusted;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDeviceId() {
        return deviceId;
    }


    public String getUserAgent() {
        return userAgent;
    }

    public String getIp() {
        return ip;
    }

    public String getToken() {
        return token;
    }

    public boolean isTrusted() {
        return isTrusted;
    }
}
