package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class UnusualHourRule implements RiskRule {

    @Override
    public int calculate(RiskContext context) {

        LocalTime now = LocalTime.now();

        if (now.isAfter(LocalTime.of(2,0))
                && now.isBefore(LocalTime.of(6,0))) {

            return 5;
        }

        return 0;
    }

}
