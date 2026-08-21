package com.beem.TastyMap.security.verification.forgotPassword;

public class PasswordRequestDTO {
    private String deviceId;
    private String identifier;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
