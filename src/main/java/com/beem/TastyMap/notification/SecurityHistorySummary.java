package com.beem.TastyMap.notification;

public interface SecurityHistorySummary {
    Long getIpAttackCount();
    Long getRejectCount();
    Long getPendingCount();
}