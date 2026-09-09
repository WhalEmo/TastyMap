package com.beem.TastyMap.security.device;

import jakarta.validation.constraints.NotBlank;

public class FcmTokenUpdateRequest {

    @NotBlank(message = "Device ID boş olamaz")
    private String deviceId;

    @NotBlank(message = "FCM Token boş olamaz")
    private String fcmToken;

    public FcmTokenUpdateRequest() {
    }

    public FcmTokenUpdateRequest(String deviceId, String fcmToken) {
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}