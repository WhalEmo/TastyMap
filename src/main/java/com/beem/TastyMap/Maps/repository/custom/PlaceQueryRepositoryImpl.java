package com.beem.TastyMap.maps.repository.custom;

import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.entity.QPlaceEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class PlaceQueryRepositoryImpl implements PlaceQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PlaceQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Map<Long, PlaceEntity> findHydrationPlacesByIds(Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QPlaceEntity place = QPlaceEntity.placeEntity;

        return queryFactory
                .selectFrom(place)
                .where(place.id.in(placeIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(PlaceEntity::getId, Function.identity()));
    }
}