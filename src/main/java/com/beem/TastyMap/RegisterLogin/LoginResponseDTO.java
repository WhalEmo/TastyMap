package com.beem.TastyMap.RegisterLogin;

public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private UserResponseDTO userResponseDTO;

    public LoginResponseDTO(String accessToken, String refreshToken, UserResponseDTO userResponseDTO) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userResponseDTO = userResponseDTO;
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
