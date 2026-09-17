package com.beem.TastyMap.mapsReview.repository;

import com.beem.TastyMap.maps.data.Review;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.entity.QReviewEntity;
import com.beem.TastyMap.mapsReview.entity.QScoreEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.beem.TastyMap.registerLogin.QUserEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ReviewQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }


    public List<ReviewResult> findReviews(String googlePlaceId, int page, int size) {
        QReviewEntity review = QReviewEntity.reviewEntity;
        return executeReviewQuery(review.place.placeId.eq(googlePlaceId), page, size);
    }

    public List<ReviewResult> findReviews(Long placeDbId, int page, int size) {
        QReviewEntity review = QReviewEntity.reviewEntity;
        return executeReviewQuery(review.place.id.eq(placeDbId), page, size);
    }

    private List<ReviewResult> executeReviewQuery(BooleanExpression placeCondition, int page, int size) {
        QReviewEntity review = QReviewEntity.reviewEntity;
        QUserEntity user = QUserEntity.userEntity;
        QScoreEntity score = QScoreEntity.scoreEntity;

        // 1. Sayfalama ve ID Filtreleme
        List<Long> reviewIds = queryFactory
                .select(review.id)
                .from(review)
                .where(
                        placeCondition,
                        review.parent.isNull(),
                        review.status.eq(ReviewStatus.APPROVED),
                        review.deleted.isFalse(),
                        review.text.isNotNull(),
                        review.text.trim().isNotEmpty()
                )
                .orderBy(review.createdAt.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        if (reviewIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Detayları ve Skorları Tek Seferde Çek
        List<Tuple> rows = queryFactory
                .select(
                        review.id,
                        user.id,
                        review.authorName,
                        user.username,
                        user.profile,
                        review.rating,
                        review.text,
                        review.source,
                        review.parent.id,
                        review.likeCount,
                        review.createdAt,
                        review.updateAt,
                        score.type,
                        score.score
                )
                .from(review)
                .leftJoin(review.user, user)
                .leftJoin(review.scores, score)
                .where(review.id.in(reviewIds))
                .orderBy(review.createdAt.desc())
                .fetch();

        // 3. ReviewResult Listesine Eşleme (Map ile Gruplama)
        Map<Long, ReviewResultBuilder> map = new LinkedHashMap<>();

        for (Tuple row : rows) {
            Long reviewId = row.get(review.id);
            ReviewSource source = row.get(review.source);

            ReviewResultBuilder builder = map.computeIfAbsent(reviewId, k -> {
                String author = (source == ReviewSource.GOOGLE)
                        ? row.get(review.authorName)
                        : row.get(user.username);

                String profileUrl = (source == ReviewSource.GOOGLE)
                        ? null
                        : row.get(user.profile);

                Long userId = (source == ReviewSource.GOOGLE) ? null : row.get(user.id);

                return new ReviewResultBuilder(
                        reviewId,
                        userId,
                        author != null ? author : "Anonim",
                        profileUrl,
                        row.get(review.rating) != null ? row.get(review.rating) : 0.0,
                        row.get(review.text),
                        source != null ? source : ReviewSource.GOOGLE,
                        row.get(review.parent.id),
                        row.get(review.likeCount) != null ? row.get(review.likeCount) : 0,
                        row.get(review.createdAt),
                        row.get(review.updateAt)
                );
            });

            if (row.get(score.type) != null) {
                builder.addScore(new ScoreDto(row.get(score.type), row.get(score.score)));
            }
        }

        return map.values().stream()
                .map(ReviewResultBuilder::build)
                .toList();
    }

}