package com.beem.TastyMap.security.verification.forgotPassword;

public class PasswordResetResponse {

    private Long userId;
    private String deviceId;
    private String message;

    public PasswordResetResponse(
            Long userId,
            String deviceId,
            String message
    ){
        this.userId = userId;
        this.deviceId = deviceId;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getMessage() { return message; }
}
