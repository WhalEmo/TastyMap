package com.beem.TastyMap.security.verification.forgotpassword.dto;

public class PasswordRequestDTO {
    private String deviceId;
    private String identifier;

    public PasswordRequestDTO() {
    }

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

