package com.beem.TastyMap.event.model;

public class SecurityAlertEvent {
        private final Long userId;
        private final String email;
        private final String deviceId;
        private final String userAgent;
        private final String ip;
        private final String token;
        private final boolean isTrusted;
        private String clientType;

    public SecurityAlertEvent(Long userId, String email, String deviceId, String userAgent, String ip, String token, boolean isTrusted,String clientType) {
        this.userId = userId;
        this.email = email;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.ip = ip;
        this.token = token;
        this.isTrusted = isTrusted;
        this.clientType = clientType;
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

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
