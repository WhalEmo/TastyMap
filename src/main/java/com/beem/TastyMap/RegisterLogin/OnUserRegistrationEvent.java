package com.beem.TastyMap.RegisterLogin;

public class OnUserRegistrationEvent {
    private final UserEntity user;
    private final String token;

    public OnUserRegistrationEvent(UserEntity user, String token) {
        this.user = user;
        this.token = token;
    }
    public UserEntity getUser() { return user; }
    public String getToken() { return token; }
}
