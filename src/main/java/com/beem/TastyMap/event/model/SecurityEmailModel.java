package com.beem.TastyMap.event.model;

import com.beem.TastyMap.registerLogin.UserEntity;

public class SecurityEmailModel {
    private final UserEntity user;
    private final String token;

    public SecurityEmailModel(UserEntity user, String token) {
        this.user = user;
        this.token = token;
    }
    public UserEntity getUser() { return user; }
    public String getToken() { return token; }
}
