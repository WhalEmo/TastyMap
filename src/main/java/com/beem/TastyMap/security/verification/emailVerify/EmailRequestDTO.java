package com.beem.TastyMap.security.verification.emailVerify;

public class EmailRequestDTO {
    private String deviceId;
    private String email;


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
