package com.beem.TastyMap.RegisterLogin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {
    @NotBlank(message = "Kullanıcı adı boş olamaz!")
    @Size(min = 3, max = 20, message = "Kullanıcı adı 3–20 karakter arasında olmalıdır")
    private String username;

    @NotBlank(message = "Parola boş olamaz!")
    private String password;
    private String deviceId;
    private String fcmToken;

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
