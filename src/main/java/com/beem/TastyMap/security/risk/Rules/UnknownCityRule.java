package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.device.UserDeviceRepo;
import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

@Service
public class UnknownCityRule implements RiskRule {

    private final UserDeviceRepo repo;

    public UnknownCityRule(UserDeviceRepo repo) {
        this.repo = repo;
    }

    @Override
    public int calculate(RiskContext context) {

        String city = context.getCurrentCity();

        if (city == null || city.isBlank()) {
            return 20;
        }

        boolean exists =
                repo.existsByUser_IdAndLastCityIgnoreCase(
                        context.getUser().getId(),
                        city
                );

        return exists ? 0 : 20;
    }
}
