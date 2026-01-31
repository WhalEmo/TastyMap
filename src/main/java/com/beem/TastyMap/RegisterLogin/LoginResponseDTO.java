package com.beem.TastyMap.RegisterLogin;

public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private UserResponseDTO userResponseDTO;
    private LoginStatus status;
    private String message;

    public LoginResponseDTO() {}
    public LoginResponseDTO(String accessToken, String refreshToken, UserResponseDTO userResponseDTO) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userResponseDTO = userResponseDTO;
        this.status = LoginStatus.SUCCESS;
        this.message = "Giriş başarılı";
    }
    public static LoginResponseDTO pendingSecurity(UserResponseDTO user) {
        LoginResponseDTO dto = new LoginResponseDTO();
        dto.status = LoginStatus.PENDING_SECURITY;
        dto.message = "Yeni cihaz tespit edildi. Güvenlik onayı bekleniyor.";
        dto.userResponseDTO = user;
        return dto;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public void setStatus(LoginStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserResponseDTO getUserResponseDTO() {
        return userResponseDTO;
    }

    public void setUserResponseDTO(UserResponseDTO userResponseDTO) {
        this.userResponseDTO = userResponseDTO;
    }
}
