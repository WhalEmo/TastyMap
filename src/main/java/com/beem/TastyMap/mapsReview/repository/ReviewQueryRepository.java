package com.beem.TastyMap.mapsReview.repository;

import com.beem.TastyMap.maps.data.Review;
import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.entity.QReviewEntity;
import com.beem.TastyMap.mapsReview.entity.QScoreEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ReviewQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }


    public List<Review> findReviewsWithScoresByPlaceId(Long placeId) {
        QReviewEntity review = QReviewEntity.reviewEntity;
        QScoreEntity score = QScoreEntity.scoreEntity;

        // 1. Tek bir LEFT JOIN sorgusu ile düz satırları çek
        List<Tuple> rows = queryFactory
                .select(
                        review.id,
                        review.source,
                        review.authorName,
                        review.rating,
                        review.text,
                        review.createdAt,
                        score.type,
                        score.score
                )
                .from(review)
                .leftJoin(review.scores, score)
                .where(
                        review.place.id.eq(placeId),
                        review.parent.isNull(),
                        review.status.eq(ReviewStatus.APPROVED),
                        review.deleted.isFalse()
                )
                .orderBy(review.createdAt.desc())
                .fetch(); // transform yerine standart fetch()

        Map<Long, ReviewBuilder> reviewMap = new LinkedHashMap<>();

        for (Tuple row : rows) {
            Long reviewId = row.get(review.id);

            ReviewBuilder builder = reviewMap.computeIfAbsent(reviewId, id -> new ReviewBuilder(
                    reviewId,
                    row.get(review.source),
                    row.get(review.authorName),
                    row.get(review.rating),
                    row.get(review.text),
                    row.get(review.createdAt)
            ));

            if (row.get(score.type) != null) {
                builder.scores.add(new ScoreDto(
                        row.get(score.type),
                        row.get(score.score)
                ));
            }
        }

        // 3. Record listesine dönüştür
        return reviewMap.values().stream()
                .map(ReviewBuilder::toReview)
                .toList();
    }

    // Gruplama için yardımcı geçici sınıf
    private static class ReviewBuilder {
        private final Long reviewId;
        private final com.beem.TastyMap.mapsReview.enums.ReviewSource source;
        private final String authorName;
        private final Double rating;
        private final String text;
        private final Long createdAt;
        private final List<ScoreDto> scores = new ArrayList<>();

        public ReviewBuilder(Long reviewId,
                             com.beem.TastyMap.mapsReview.enums.ReviewSource source,
                             String authorName,
                             Double rating,
                             String text,
                             Long createdAt) {
            this.reviewId = reviewId;
            this.source = source;
            this.authorName = authorName;
            this.rating = rating;
            this.text = text;
            this.createdAt = createdAt;
        }

        public Review toReview() {
            return new Review(reviewId, source, authorName, rating, text, createdAt, scores);
        }
    }

}