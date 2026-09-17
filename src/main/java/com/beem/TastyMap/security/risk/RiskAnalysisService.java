package com.beem.TastyMap.security.risk;
import com.beem.TastyMap.user.account.entity.UserEntity;
import com.beem.TastyMap.security.Location.GeoLocationService;
import com.beem.TastyMap.security.device.entity.UserDeviceEntity;
import com.beem.TastyMap.security.device.repo.UserDeviceRepo;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class RiskAnalysisService {

    private final UserDeviceRepo userDeviceRepo;
    private final GeoLocationService geoLocationService;
    private final List<RiskRule> rules;

    public RiskAnalysisService(UserDeviceRepo userDeviceRepo, GeoLocationService geoLocationService, List<RiskRule> rules) {
        this.userDeviceRepo = userDeviceRepo;
        this.geoLocationService = geoLocationService;
        this.rules = rules;
    }

    public RiskResult calculateRiskScore(UserEntity user,
                                         String ip,
                                         String deviceId) {

        UserDeviceEntity device =
                userDeviceRepo.findByUser_IdAndDeviceId(user.getId(), deviceId)
                        .orElse(null);

        if (device != null && device.isTrusted()) {
            return new RiskResult(0, null);
        }

        if (!userDeviceRepo.existsByUser_Id(user.getId())) {
            return new RiskResult(0, null);
        }

        String city = geoLocationService.getCity(ip);

        RiskContext context =
                new RiskContext(
                        user,
                        device,
                        ip,
                        city
                );

        int score = 0;

        for (RiskRule rule : rules) {
            score += rule.calculate(context);
        }

        return new RiskResult(score, null);
    }


}