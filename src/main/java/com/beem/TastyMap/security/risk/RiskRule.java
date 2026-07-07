package com.beem.TastyMap.security.risk;

public interface RiskRule {

    int calculate(RiskContext context);

}