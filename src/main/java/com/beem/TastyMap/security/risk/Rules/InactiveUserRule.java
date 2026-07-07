package com.beem.TastyMap.security.risk.Rules;

import com.beem.TastyMap.security.risk.RiskContext;
import com.beem.TastyMap.security.risk.RiskRule;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InactiveUserRule implements RiskRule {

    @Override
    public int calculate(RiskContext context) {

        if (context.getUser().getLastInteractionAt() == null) {
            return 0;
        }

        if (context.getUser()
                .getLastInteractionAt()
                .isBefore(LocalDateTime.now().minusDays(30))) {

            return 10;
        }

        return 0;
    }

}
