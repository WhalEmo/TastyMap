package com.beem.TastyMap.rag.data.dto;

import java.util.List;

public record IndexedPlaceDTO(
        Long id,
        String placeId,
        String name,
        String formattedAddress,
        Double googleRating,
        Double tastymapRating,
        Double latitude,
        Double longitude,
        Boolean isOpenNow,
        List<IndexedReviewDTO> reviews
) {}