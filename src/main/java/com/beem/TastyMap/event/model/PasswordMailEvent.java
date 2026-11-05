package com.beem.TastyMap.event.model;

public class PasswordMailEvent {
    private final String Email;
    private final String token;
    private String clientType;

    public PasswordMailEvent(String email, String token, String clientType) {
        this.Email = email;
        this.token = token;
        this.clientType = clientType;
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

