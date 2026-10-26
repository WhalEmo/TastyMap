package com.beem.TastyMap.rag.data.request;

public record RecommendationReq(
        String sessionId,
        String query,
        boolean ignoreAllergies,
        Boolean isOpenNow,
        Double latitude,
        Double longitude,
        Double radiusKm,
        boolean isMoreRequest
) {
    public RecommendationReq {
        if (radiusKm == null) {
            radiusKm = 5.0;
        }
    }
}