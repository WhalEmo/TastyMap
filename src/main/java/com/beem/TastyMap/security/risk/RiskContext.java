package com.beem.TastyMap.security.risk;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.device.UserDeviceEntity;

public class RiskContext {

    private final UserEntity user;
    private final UserDeviceEntity device;
    private final String currentIp;
    private final String currentCity;

    public RiskContext(UserEntity user,
                       UserDeviceEntity device,
                       String currentIp,
                       String currentCity) {

        this.user = user;
        this.device = device;
        this.currentIp = currentIp;
        this.currentCity = currentCity;

    }

    public UserEntity getUser() {
        return user;
    }

    public UserDeviceEntity getDevice() {
        return device;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    public String getCurrentCity() {
        return currentCity;
    }

}