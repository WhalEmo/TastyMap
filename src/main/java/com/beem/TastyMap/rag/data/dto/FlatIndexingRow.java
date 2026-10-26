package com.beem.TastyMap.rag.data.dto;

import com.beem.TastyMap.mapsReview.enums.ScoreType;

public record FlatIndexingRow(
        Long placeDbId,
        String placeId,
        String name,
        String formattedAddress,
        Double googleRating,
        Double tastyMapRating,
        Double latitude,
        Double longitude,
        Boolean isOpenNow,
        Long reviewId,
        String reviewText,
        ScoreType scoreType,
        Double scoreValue
) {}