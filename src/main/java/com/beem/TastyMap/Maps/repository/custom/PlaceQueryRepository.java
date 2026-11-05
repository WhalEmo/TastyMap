package com.beem.TastyMap.maps.repository.custom;

import com.beem.TastyMap.maps.entity.PlaceEntity;

import java.util.Collection;
import java.util.Map;

public interface PlaceQueryRepository {
    Map<Long, PlaceEntity> findHydrationPlacesByIds(Collection<Long> placeIds);
}