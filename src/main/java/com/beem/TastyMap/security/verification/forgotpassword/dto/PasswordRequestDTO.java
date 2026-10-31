package com.beem.TastyMap.security.verification.forgotpassword.dto;

public class PasswordRequestDTO {
    private String deviceId;
    private String identifier;

    public PasswordRequestDTO(String deviceId, String identifier) {
        this.deviceId = deviceId;
        this.identifier = identifier;
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

