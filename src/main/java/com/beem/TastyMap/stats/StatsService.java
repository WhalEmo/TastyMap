package com.beem.TastyMap.stats;

import com.beem.TastyMap.mapsReview.entity.QReviewEntity;
import com.beem.TastyMap.mapsReview.entity.QScoreEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.beem.TastyMap.stats.data.response.PlaceStatsDto;
import com.beem.TastyMap.stats.data.response.ScoreMetricDto;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final JPAQueryFactory queryFactory;

    public StatsService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public PlaceStatsDto calculatePlaceStats(Long placeId, Double overallRating, Integer totalReviewCount) {
        if (totalReviewCount == null || totalReviewCount == 0) {
            return new PlaceStatsDto(0.0, 0, Map.of(), List.of(), List.of());
        }

        QReviewEntity review = QReviewEntity.reviewEntity;
        QScoreEntity score = QScoreEntity.scoreEntity;

        // 1. Yıldız Dağılımı (1..5) - Querydsl
        List<Tuple> starTuples = queryFactory
                .select(review.rating.floor(), review.id.count())
                .from(review)
                .where(
                        review.place.id.eq(placeId),
                        review.source.eq(ReviewSource.INTERNAL),
                        review.status.eq(ReviewStatus.APPROVED),
                        review.parent.isNull()
                )
                .groupBy(review.rating.floor())
                .fetch();

        Map<Integer, Integer> starMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) starMap.put(i, 0);

        for (Tuple tuple : starTuples) {
            Double ratingFloor = tuple.get(review.rating.floor());
            Long count = tuple.get(review.id.count());
            if (ratingFloor != null && count != null) {
                int star = ratingFloor.intValue();
                if (star >= 1 && star <= 5) {
                    starMap.put(star, count.intValue());
                }
            }
        }

        // 2. Kriter Ortalamaları - Querydsl
        List<Tuple> scoreTuples = queryFactory
                .select(score.type, score.score.avg(), score.id.count())
                .from(score)
                .join(score.review, review)
                .where(
                        review.place.id.eq(placeId),
                        review.source.eq(ReviewSource.INTERNAL),
                        review.status.eq(ReviewStatus.APPROVED),
                        score.type.ne(ScoreType.OVERALL)
                )
                .groupBy(score.type)
                .fetch();

        List<ScoreMetricDto> criteriaMetrics = new ArrayList<>();
        List<String> highlights = new ArrayList<>();

        for (Tuple tuple : scoreTuples) {
            ScoreType type = tuple.get(score.type);
            Double rawAvg = tuple.get(score.score.avg());
            Long count = tuple.get(score.id.count());

            if (type != null && rawAvg != null && count != null) {
                double avg = Math.round(rawAvg * 10.0) / 10.0;
                int intCount = count.intValue();

                criteriaMetrics.add(new ScoreMetricDto(type, type.name(), avg, intCount));

                if (avg >= 4.5 && intCount >= 1) {
                    highlights.add(type.name() + " Öne Çıkıyor");
                }
            }
        }

        return new PlaceStatsDto(
                overallRating != null ? overallRating : 0.0,
                totalReviewCount,
                starMap,
                criteriaMetrics,
                highlights
        );
    }
}