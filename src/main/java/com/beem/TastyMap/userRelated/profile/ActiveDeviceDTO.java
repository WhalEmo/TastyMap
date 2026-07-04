package com.beem.TastyMap.userRelated.profile;

import java.time.LocalDateTime;

public class ActiveDeviceDTO {
    private String deviceId;
    private String userAgent;
    private String city;
    private LocalDateTime lastUsedAt;

    public ActiveDeviceDTO(String deviceId,String userAgent, String city,LocalDateTime lastUsedAt) {
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.city= city;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
