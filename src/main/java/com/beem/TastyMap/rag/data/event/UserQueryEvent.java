package com.beem.TastyMap.rag.data.event;

public record UserQueryEvent(
        String sessionId,
        Long userId,
        String userQuery,
        Double latitude,
        Double longitude,
        Double radiusKm,
        boolean ignoreHealthProfile,
        String provider
) {}