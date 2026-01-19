package com.beem.TastyMap.Security.DeviceCount;

import java.time.LocalDateTime;

public class ActiveDeviceDTO {
    private String deviceId;
    private String userAgent;
    private LocalDateTime lastUsedAt;

    public ActiveDeviceDTO(String deviceId,String userAgent, LocalDateTime lastUsedAt) {
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.lastUsedAt = lastUsedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
