package com.beem.TastyMap.security.device;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.Location.GeoLocationService;
import com.beem.TastyMap.security.util.IpUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class UserDeviceService {
    private final UserDeviceRepo userDeviceRepo;
    private final GeoLocationService geoLocationService;

    public UserDeviceService(UserDeviceRepo userDeviceRepo, GeoLocationService geoLocationService) {
        this.userDeviceRepo = userDeviceRepo;
        this.geoLocationService = geoLocationService;
    }

    @Transactional
    public void registerOrUpdateDevice(UserEntity user, String deviceId, String userAgent, String fcmToken, boolean isTrusted) {
        UserDeviceEntity device = userDeviceRepo.findByUser_IdAndDeviceId(user.getId(), deviceId)
                .orElse(new UserDeviceEntity());

        String ip = IpUtils.getClientIp();

        device.setUser(user);
        device.setDeviceId(deviceId);
        device.setUserAgent(userAgent);
        device.setLastIpAddress(ip);
        device.setLastCity(geoLocationService.getCity(ip));
        device.setLastLoginAt(LocalDateTime.now());
        device.setTrusted(isTrusted);

        if (fcmToken != null) {
            device.setFcmToken(fcmToken);
        }

        userDeviceRepo.save(device);
    }
}