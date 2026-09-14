package com.beem.TastyMap.securitynotification;

public interface SecurityHistorySummary {
    Long getIpAttackCount();
    Long getRejectCount();
    Long getPendingCount();
}