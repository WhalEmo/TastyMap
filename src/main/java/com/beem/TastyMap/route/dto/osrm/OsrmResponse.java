package com.beem.TastyMap.route.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmResponse(
        String code,
        List<OsrmRoute> routes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmRoute(
            Double distance,
            Double duration,
            OsrmGeometry geometry
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmGeometry(
            List<List<Double>> coordinates,
            String type
    ) {}
}