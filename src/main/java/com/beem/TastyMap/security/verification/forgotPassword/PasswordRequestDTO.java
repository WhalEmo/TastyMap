package com.beem.TastyMap.security.verification.forgotPassword;

public class PasswordRequestDTO {
    private Long userId;
    private String currentIp;
    private String deviceId;
    private String fingerPritnhash;
    private String email;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    public void setCurrentIp(String currentIp) {
        this.currentIp = currentIp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getFingerPritnhash() {
        return fingerPritnhash;
    }

    public void setFingerPritnhash(String fingerPritnhash) {
        this.fingerPritnhash = fingerPritnhash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
