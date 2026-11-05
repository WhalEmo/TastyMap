package com.beem.TastyMap.rag.data.response;

import java.util.List;

public record HydratedPlaceCardDto(
        Long dbId,
        String placeId,
        String name,
        String vicinity,
        Double latitude,
        Double longitude,
        Double googleRating,
        Double tastyMapRating,
        Integer priceLevel,
        Boolean isOpenNow,
        String businessStatus,
        List<String> highlightedFoods,
        boolean isAvailable // Mekan DB'den silinmişse false döner
) {}