package com.beem.TastyMap.notification;

import com.beem.TastyMap.registerLogin.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_user_device_status", columnList = "user_id, deviceId, status, createdAt"),
        @Index(name = "idx_device_token", columnList = "deviceId, token"),
        @Index(name = "idx_device_fingerprint_used", columnList = "deviceId, fingerprintHash, isUsed, createdAt DESC")
})
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private String deviceId;
    private String userAgent;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String lastIpAddress;
    private String lastCity;
    private boolean isTrusted = false;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String token;
    private boolean isUsed;
    private LocalDateTime updatedAt;

    private String fingerPrintHash;

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastIpAddress() {
        return lastIpAddress;
    }

    public void setLastIpAddress(String lastIpAddress) {
        this.lastIpAddress = lastIpAddress;
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


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFingerPrintHash() {
        return fingerPrintHash;
    }

    public void setFingerPrintHash(String fingerPrintHash) {
        this.fingerPrintHash = fingerPrintHash;
    }
}
