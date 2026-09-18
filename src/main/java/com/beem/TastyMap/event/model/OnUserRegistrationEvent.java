package com.beem.TastyMap.event.model;

public class OnUserRegistrationEvent {
    private final String Email;
    private final String token;

    public OnUserRegistrationEvent(String email, String token) {
        Email = email;
        this.token = token;
    }

    public String getEmail() {
        return Email;
    }

    public String getToken() {
        return token;
    }
}
