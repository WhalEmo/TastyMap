package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class FingerprintRule implements RiskRule {

    @Override
    public int calculate(RiskContext context) {

        if (context.getDevice() == null) {
            return 0;
        }

        if (!Objects.equals(
                context.getDevice().getFingerprintHash(),
                context.getFingerprintHash())) {

            return 60;
        }

        return 0;
    }
}
