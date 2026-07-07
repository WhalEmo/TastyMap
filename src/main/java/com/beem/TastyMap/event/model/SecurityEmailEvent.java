package com.beem.TastyMap.event.model;

import com.beem.TastyMap.registerLogin.UserEntity;

public class SecurityEmailEvent {
    private final String Email;
    private final String token;

    public SecurityEmailEvent(String email, String token) {
        Email = email;
        this.token = token;
    }

    public String getEmail() {
        return Email;
    }

    public String getToken() { return token; }
}
