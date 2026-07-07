package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

@Service
public class TrustedDeviceRule implements RiskRule {

    @Override
    public int calculate(RiskContext context) {

        if (context.getDevice() == null) {
            return 0;
        }

        if (!context.getDevice().isTrusted()) {
            return 10;
        }

        return 0;
    }
}