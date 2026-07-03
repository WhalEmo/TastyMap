package com.beem.TastyMap.notification;

public interface NotificationStatusSummary {
    String getFingerPrintHash();
    Status getStatus();
    boolean isUsed();
}
