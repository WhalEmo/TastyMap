package com.beem.TastyMap.event.model;

import com.beem.TastyMap.registerLogin.LoginRequestDTO;
import com.beem.TastyMap.registerLogin.UserEntity;

public class SecurityAlertEvent {
    private final UserEntity user;
    private final LoginRequestDTO dto;
    private final String userAgent;
    private final String ip;
    private final String token;
    private final boolean isTrusted;

    public SecurityAlertEvent(UserEntity user, LoginRequestDTO dto, String userAgent, String ip, String token, boolean isTrusted) {
        this.user = user;
        this.dto = dto;
        this.userAgent = userAgent;
        this.ip = ip;
        this.token = token;
        this.isTrusted = isTrusted;
    }

    public UserEntity getUser() {
        return user;
    }

    public LoginRequestDTO getDto() {
        return dto;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIp() {
        return ip;
    }

    public String getToken() {
        return token;
    }


    public boolean isTrusted() {
        return isTrusted;
    }
}
