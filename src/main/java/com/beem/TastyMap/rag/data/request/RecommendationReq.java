package com.beem.TastyMap.rag.data.request;

import jakarta.validation.constraints.NotBlank;

public record RecommendationReq(
        String sessionId,

        @NotBlank(message = "Arama sorgusu boş olamaz")
        String query,

        Double latitude,
        Double longitude,
        Double radiusKm,
        Boolean ignoreHealthProfile,
        String provider
) {
    public RecommendationReq {
        if (radiusKm == null || radiusKm <= 0) {
            radiusKm = 5.0; // Varsayılan arama yarıçapı: 5 km
        }
        if (ignoreHealthProfile == null) {
            ignoreHealthProfile = Boolean.FALSE;
        }
        if (sessionId != null && sessionId.isBlank()) {
            sessionId = null;
        }
    }
}