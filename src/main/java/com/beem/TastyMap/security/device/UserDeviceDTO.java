package com.beem.TastyMap.security.device;


import jakarta.persistence.Persistence;

import java.time.LocalDateTime;

public class UserDeviceDTO {
    private Long id;
    private Long userId;
    private String deviceId;
    private String fingerprintHash;
    private String userAgent;
    private String lastIpAddress;
    private String fcmToken;
    private String lastCity;
    private boolean isTrusted;
    private LocalDateTime lastLoginAt;

    public UserDeviceDTO() {
    }

    public UserDeviceDTO(Long id, Long userId, String deviceId, String fingerprintHash,
                         String userAgent, String lastIpAddress, String fcmToken,
                         String lastCity, boolean isTrusted, LocalDateTime lastLoginAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.fingerprintHash = fingerprintHash;
        this.userAgent = userAgent;
        this.lastIpAddress = lastIpAddress;
        this.fcmToken = fcmToken;
        this.lastCity = lastCity;
        this.isTrusted = isTrusted;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserDeviceDTO fromEntity(UserDeviceEntity entity) {
        if (entity == null) {
            return null;
        }

        Long userId = null;
        if (entity.getUser() != null && Persistence.getPersistenceUtil().isLoaded(entity.getUser())) {
            userId = entity.getUser().getId();
        }

        return new UserDeviceDTO(
                entity.getId(),
                userId,
                entity.getDeviceId(),
                entity.getFingerprintHash(),
                entity.getUserAgent(),
                entity.getLastIpAddress(),
                entity.getFcmToken(),
                entity.getLastCity(),
                entity.isTrusted(),
                entity.getLastLoginAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getFingerprintHash() {
        return fingerprintHash;
    }

    public void setFingerprintHash(String fingerprintHash) {
        this.fingerprintHash = fingerprintHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getLastIpAddress() {
        return lastIpAddress;
    }

    public void setLastIpAddress(String lastIpAddress) {
        this.lastIpAddress = lastIpAddress;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getLastCity() {
        return lastCity;
    }

    public void setLastCity(String lastCity) {
        this.lastCity = lastCity;
    }

    public boolean isTrusted() {
        return isTrusted;
    }

    public void setTrusted(boolean trusted) {
        isTrusted = trusted;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}