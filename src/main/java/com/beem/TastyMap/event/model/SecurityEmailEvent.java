package com.beem.TastyMap.event.model;

public class SecurityEmailEvent {
    private final String Email;
    private final String token;
    private String clientType;

    public SecurityEmailEvent(String clientType, String token, String email) {
        this.clientType = clientType;
        this.token = token;
        Email = email;
    }

    public String getEmail() {
        return Email;
    }

    public String getToken() {
        return token;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
