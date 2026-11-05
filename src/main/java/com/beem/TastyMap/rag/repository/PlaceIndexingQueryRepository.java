package com.beem.TastyMap.rag.repository;

import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.entity.QPlaceEntity;
import com.beem.TastyMap.mapsReview.entity.QReviewEntity;
import com.beem.TastyMap.mapsReview.entity.QScoreEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.beem.TastyMap.rag.data.dto.*;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PlaceIndexingQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PlaceIndexingQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Transactional(readOnly = true)
    public List<IndexedPlaceDTO> fetchNextChunk(Long lastSeenId, int chunkSize) {
        QPlaceEntity place = QPlaceEntity.placeEntity;
        QReviewEntity review = QReviewEntity.reviewEntity;
        QScoreEntity score = QScoreEntity.scoreEntity;

        // 1. Keyset Cursor: LIMIT mekan sayısına göre index scan
        List<Long> chunkPlaceIds = queryFactory
                .select(place.id)
                .from(place)
                .where(place.id.gt(lastSeenId))
                .orderBy(place.id.asc())
                .limit(chunkSize)
                .fetch();

        if (chunkPlaceIds.isEmpty()) {
            return List.of();
        }

        // 2. Hibernate 6 Güvenli Projeksiyon (ScrollableResults YOK, düz fetch)
        List<FlatIndexingRow> rows = queryFactory
                .select(Projections.constructor(
                        FlatIndexingRow.class,
                        place.id,
                        place.placeId,
                        place.name,
                        place.formattedAddress,
                        place.googleRating,
                        place.tastyMapRating,
                        place.latitude,
                        place.longitude,
                        place.openingHours.openNow,
                        review.id,
                        review.text,
                        score.type,
                        score.score
                ))
                .from(place)
                .leftJoin(place.reviews, review)
                .on(review.parent.isNull()
                        .and(review.status.eq(ReviewStatus.APPROVED))
                        .and(review.deleted.isFalse())
                        .and(review.text.isNotNull()))
                .leftJoin(review.scores, score)
                .where(place.id.in(chunkPlaceIds))
                .orderBy(place.id.asc())
                .fetch();

        // 3. Bellekte Ağaç (Tree) Yapısına Dönüştür
        return mapRowsToIndexedPlaceDTOs(chunkPlaceIds, rows);
    }

    private List<IndexedPlaceDTO> mapRowsToIndexedPlaceDTOs(List<Long> orderedPlaceIds, List<FlatIndexingRow> rows) {
        // Mekan ID sırasını korumak için LinkedHashMap
        Map<Long, List<FlatIndexingRow>> rowsByPlace = rows.stream()
                .collect(Collectors.groupingBy(FlatIndexingRow::placeDbId, LinkedHashMap::new, Collectors.toList()));

        List<IndexedPlaceDTO> resultList = new ArrayList<>(orderedPlaceIds.size());

        for (Long placeId : orderedPlaceIds) {
            List<FlatIndexingRow> placeRows = rowsByPlace.get(placeId);
            if (placeRows == null || placeRows.isEmpty()) {
                continue;
            }

            FlatIndexingRow first = placeRows.get(0);

            // Review ID bazında yorum ve skorları birleştir
            Map<Long, List<IndexedScoreDTO>> scoresByReviewId = new HashMap<>();
            Map<Long, String> textByReviewId = new LinkedHashMap<>();

            for (FlatIndexingRow row : placeRows) {
                if (row.reviewId() != null) {
                    textByReviewId.putIfAbsent(row.reviewId(), row.reviewText());
                    if (row.scoreType() != null && row.scoreValue() != null) {
                        scoresByReviewId.computeIfAbsent(row.reviewId(), k -> new ArrayList<>())
                                .add(new IndexedScoreDTO(row.scoreType(), row.scoreValue()));
                    }
                }
            }

            List<IndexedReviewDTO> reviewDTOs = textByReviewId.entrySet().stream()
                    .map(entry -> new IndexedReviewDTO(
                            entry.getKey(),
                            entry.getValue(),
                            scoresByReviewId.getOrDefault(entry.getKey(), List.of())
                    ))
                    .toList();

            resultList.add(new IndexedPlaceDTO(
                    first.placeDbId(),
                    first.placeId(),
                    first.name(),
                    first.formattedAddress(),
                    first.googleRating(),
                    first.tastyMapRating(),
                    first.latitude(),
                    first.longitude(),
                    first.isOpenNow(),
                    reviewDTOs
            ));
        }

        return resultList;
    }

    @Transactional(readOnly = true)
    public Map<Long, Set<String>> fetchTypesForPlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Map.of();
        }

        QPlaceEntity place = QPlaceEntity.placeEntity;

        List<PlaceEntity> places = queryFactory
                .selectFrom(place)
                .where(place.id.in(placeIds))
                .fetch();

        return places.stream()
                .collect(Collectors.toMap(
                        PlaceEntity::getId,
                        PlaceEntity::getTypes,
                        (existing, replacement) -> existing
                ));
    }
}