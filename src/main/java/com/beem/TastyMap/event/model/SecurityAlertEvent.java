package com.beem.TastyMap.event.model;

import com.beem.TastyMap.registerLogin.LoginRequestDTO;
import com.beem.TastyMap.registerLogin.UserEntity;

public class SecurityAlertEvent {
    private final UserEntity user;
    private final LoginRequestDTO dto;
    private final String userAgent;
    private final String ip;

    public SecurityAlertEvent(UserEntity user, LoginRequestDTO dto, String userAgent, String ip) {
        this.user = user;
        this.dto = dto;
        this.userAgent = userAgent;
        this.ip = ip;
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
}
