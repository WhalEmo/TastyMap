package com.beem.TastyMap.security.risk;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.security.Location.GeoLocationService;
import com.beem.TastyMap.security.device.UserDeviceDTO;
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

    public RiskAnalysisService(UserDeviceRepo userDeviceRepo, GeoLocationService geoLocationService) {
        this.userDeviceRepo = userDeviceRepo;
        this.geoLocationService = geoLocationService;
    }

    public RiskResult calculateRiskScore(UserEntity user, String currentIp, String deviceId, String fingerprintHash) {
        UserDeviceEntity device = userDeviceRepo
                .findByUser_IdAndDeviceId(user.getId(), deviceId)
                .orElse(null);

        UserDeviceDTO deviceDto = UserDeviceDTO.fromEntity(device);

        if (device != null && device.isTrusted()) {
            return new RiskResult(0, deviceDto);
        }

        if (!userDeviceRepo.existsByUser_Id(user.getId())) {
            return new RiskResult(0, deviceDto);
        }

        int risk = 0;

        if (device == null) {
            risk += 50;
        } else {
            if (!java.util.Objects.equals(device.getFingerprintHash(), fingerprintHash)) {
                risk += 60;
            }
            if (!device.isTrusted()) {
                risk += 10;
            }
        }

        String currentCity = geoLocationService.getCity(currentIp);
        if (currentCity != null && !currentCity.isBlank()) {
            boolean isKnownCity = userDeviceRepo.existsByUser_IdAndLastCityIgnoreCase(user.getId(), currentCity);
            if (!isKnownCity) {
                risk += 20;
            }
        } else {
            risk += 20;
        }

        String subnetPattern = getSubnetPattern(currentIp);
        if (subnetPattern != null) {
            boolean isKnownNetwork = userDeviceRepo.existsByUser_IdAndSubnet(user.getId(), subnetPattern);
            if (!isKnownNetwork) {
                risk += 20;
            }
        } else {
            risk += 20;
        }

        if (user.getLastInteractionAt() != null
                && user.getLastInteractionAt().isBefore(LocalDateTime.now().minusDays(30))) {
            risk += 10;
        }

        if (isUnusualHour(LocalTime.now())) {
            risk += 5;
        }

        return new RiskResult(risk, deviceDto);
    }


    private String getSubnetPattern(String ip) {
        if (ip == null || !ip.contains(".")) {
            return null;
        }
        try {
            return ip.substring(0, ip.lastIndexOf('.')) + ".%";
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isUnusualHour(LocalTime time) {
        return time.isAfter(LocalTime.of(2, 0)) && time.isBefore(LocalTime.of(6, 0));
    }
}