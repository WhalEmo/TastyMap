package com.beem.TastyMap.rag.data.event;

public record UserQueryEvent(
        String sessionId,
        Long userId,
        String userQuery,
        boolean ignoreAllergies,
        Boolean isOpenNow,
        Double latitude,
        Double longitude,
        Double radiusKm
) {
}