package com.beem.TastyMap.Security;

public class RefreshTokenResponseDTO {
    private String accessToken;
    private String refreshtoken;
    private String message;

    public RefreshTokenResponseDTO(String accessToken,String refreshtoken,String message) {
        this.accessToken = accessToken;
        this.refreshtoken=refreshtoken;
        this.message=message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRefreshtoken() {
        return refreshtoken;
    }

    public void setRefreshtoken(String refreshtoken) {
        this.refreshtoken = refreshtoken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
