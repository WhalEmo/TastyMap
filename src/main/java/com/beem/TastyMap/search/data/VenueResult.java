package com.beem.TastyMap.search.data;

public record VenueResult(
        Long id,
        String placeId,
        String name,
        String vicinity,
        Double lat,
        Double lng,
        Double googleRating,
        Double tastyMapRating
) {}