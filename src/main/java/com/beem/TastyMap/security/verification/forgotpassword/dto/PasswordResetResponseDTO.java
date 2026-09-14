package com.beem.TastyMap.security.verification.forgotpassword.dto;

public class PasswordResetResponseDTO {

    private Long userId;
    private String deviceId;
    private String message;

    public PasswordResetResponseDTO(
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
