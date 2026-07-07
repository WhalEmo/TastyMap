package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.device.UserDeviceRepo;
import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

@Service
public class UnknownSubnetRule implements RiskRule {

    private final UserDeviceRepo repo;

    public UnknownSubnetRule(UserDeviceRepo repo) {
        this.repo = repo;
    }

    @Override
    public int calculate(RiskContext context) {

        String subnet = getSubnet(context.getCurrentIp());

        if (subnet == null) {
            return 20;
        }

        boolean exists =
                repo.existsByUser_IdAndSubnet(
                        context.getUser().getId(),
                        subnet);

        return exists ? 0 : 20;
    }

    private String getSubnet(String ip) {

        if (ip == null || !ip.contains(".")) {
            return null;
        }

        return ip.substring(0, ip.lastIndexOf('.')) + ".%";
    }

}