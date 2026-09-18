package com.beem.TastyMap.route.dto;

import java.util.List;

public record RouteDirectionResponse(
        Double distanceMeters,
        Double durationSeconds,
        String formattedDistance,
        String formattedDuration,
        List<List<Double>> coordinates
) {
    public static RouteDirectionResponse empty() {
        return new RouteDirectionResponse(0.0, 0.0, "0 m", "0 dk", List.of());
    }
}