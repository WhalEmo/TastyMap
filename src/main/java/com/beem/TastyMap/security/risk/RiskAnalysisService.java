package com.beem.TastyMap.security.risk;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.Location.GeoLocationService;
import com.beem.TastyMap.security.device.UserDeviceEntity;
import com.beem.TastyMap.security.device.UserDeviceRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RiskAnalysisService {

    private final UserDeviceRepo userDeviceRepo;
    private final GeoLocationService geoLocationService;

    public RiskAnalysisService(
            UserDeviceRepo userDeviceRepo,
            GeoLocationService geoLocationService
    ) {
        this.userDeviceRepo = userDeviceRepo;
        this.geoLocationService = geoLocationService;
    }

    public int calculateRiskScore(UserEntity user, String currentIp, String deviceId, String fingerprintHash) {
        UserDeviceEntity device = userDeviceRepo
                .findByUser_IdAndDeviceId(user.getId(), deviceId)
                .orElse(null);

        if (device != null && device.isTrusted()) {
            return 0;
        }

        List<UserDeviceEntity> userDevices =
                userDeviceRepo.findByUser_Id(user.getId());

        if (userDevices.isEmpty()) {
            return 0;
        }

        int risk = 0;

        if (device == null) {
            risk += 50;
        } else {

            if (!java.util.Objects.equals(
                    device.getFingerprintHash(),
                    fingerprintHash)) {

                risk += 60;
            }

            if (!device.isTrusted()) {
                risk += 10;
            }
        }

        String currentCity =
                geoLocationService.getCity(currentIp);

        boolean isKnownCity = userDevices.stream()
                .anyMatch(d ->
                        d.getLastCity() != null &&
                                d.getLastCity().equalsIgnoreCase(currentCity));

        if (!isKnownCity) {
            risk += 20;
        }

        boolean isKnownNetwork = userDevices.stream()
                .anyMatch(d ->
                        d.getLastIpAddress() != null &&
                                isSameNetwork(d.getLastIpAddress(), currentIp));

        if (!isKnownNetwork) {
            risk += 20;
        }

        if (user.getLastInteractionAt() != null
                && user.getLastInteractionAt()
                .isBefore(LocalDateTime.now().minusDays(30))) {

            risk += 10;
        }

        if (isUnusualHour(LocalTime.now())) {
            risk += 5;
        }

        return risk;
    }

    private boolean isSameNetwork(String ip1, String ip2) {

        if (ip1 == null || ip2 == null) {
            return false;
        }

        if (!ip1.contains(".") || !ip2.contains(".")) {
            return ip1.equals(ip2);
        }

        try {
            String subnet1 =
                    ip1.substring(0, ip1.lastIndexOf('.'));

            String subnet2 =
                    ip2.substring(0, ip2.lastIndexOf('.'));

            return subnet1.equals(subnet2);

        } catch (Exception e) {
            return ip1.equals(ip2);
        }
    }

    private boolean isUnusualHour(LocalTime time) {

        return time.isAfter(LocalTime.of(2, 0))
                && time.isBefore(LocalTime.of(6, 0));
    }
}