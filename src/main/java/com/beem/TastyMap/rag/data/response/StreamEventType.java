package com.beem.TastyMap.rag.data.response;

import java.util.Locale;

public enum StreamEventType {
    STATUS("status", false),
    TOKEN("token", false),
    PLACES("places", false),
    QUOTA_INFO("quota_info", false),
    COMPLETED("completed", true),
    ERROR("error", true);

    private final String eventName;
    private final boolean isFinal;

    StreamEventType(String eventName, boolean isFinal) {
        this.eventName = eventName;
        this.isFinal = isFinal;
    }

    public String getEventName() {
        return eventName;
    }

    public boolean isFinal() {
        return isFinal;
    }
}