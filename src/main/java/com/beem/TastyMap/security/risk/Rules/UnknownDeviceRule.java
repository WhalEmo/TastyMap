package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

@Service
public class UnknownDeviceRule implements RiskRule {
    @Override
    public int calculate(RiskContext context) {

        if (context.getDevice() == null) {
            return 50;
        }

        return 0;
    }
}