package com.beem.TastyMap.mapsReview.repository;

import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReviewRepo extends JpaRepository<ReviewEntity, Long> {

    long countByPlace_PlaceIdAndTimeGreaterThanEqual(String placeId, Long minTime);
    long countByPlace_PlaceIdAndRatingGreaterThanEqualAndTimeGreaterThanEqual(String placeId, Double minRating, Long minTime);
    long countByPlace_PlaceIdAndRatingBetweenAndTimeGreaterThanEqual(String placeId, Double minRating, Double maxRating, Long minTime);
    long countByPlace_PlaceIdAndRatingLessThanAndTimeGreaterThanEqual(String placeId, Double maxRating, Long minTime);

    @Query("SELECT r FROM ReviewEntity r WHERE r.place.placeId = :placeId AND r.rating >= :minRating AND r.rating <= :maxRating AND r.time >= :minTime AND LENGTH(r.text) >= :minLength ORDER BY r.likeCount DESC, r.time DESC")
    List<ReviewEntity> findQualityReviewsByRatingRangeAndTime(
            @Param("placeId") String placeId,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            @Param("minTime") Long minTime,
            @Param("minLength") Integer minLength,
            Pageable pageable
    );

    @Query("SELECT r FROM ReviewEntity r WHERE r.place.placeId = :placeId AND r.rating >= :minRating AND r.rating <= :maxRating AND r.time >= :minTime AND LENGTH(r.text) >= :minLength AND LENGTH(r.text) < :maxLength ORDER BY r.likeCount DESC, r.time DESC")
    List<ReviewEntity> findFallbackReviews(
            @Param("placeId") String placeId,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            @Param("minTime") Long minTime,
            @Param("minLength") Integer minLength,
            @Param("maxLength") Integer maxLength,
            Pageable pageable
    );

    @Query("SELECT r FROM ReviewEntity r LEFT JOIN FETCH r.user WHERE r.place.placeId = :placeId")
    List<ReviewEntity> findAllByPlaceId(@Param("placeId") String placeId, Pageable pageable);

    Page<ReviewEntity> findByPlace_PlaceIdAndStatus(String placeId, ReviewStatus status, Pageable pageable);

    boolean existsByIdAndPlaceId(Long id, Long placeId);

    boolean existsByIdAndPlaceIdAndSourceAndStatus(
            Long id,
            Long placeId,
            ReviewSource source,
            ReviewStatus status
    );

    Optional<ReviewEntity> findByIdAndUserId(
            Long id,
            Long userId
    );

    Optional<ReviewEntity> findFirstByPlace_PlaceIdAndUserIdAndParentIsNullAndStatusOrderByCreatedAtDesc(
            String placeId,
            Long userId,
            ReviewStatus status
    );

    boolean existsByPlace_PlaceIdAndUserIdAndParentIsNullAndStatus(
            String placeId,
            Long userId,
            ReviewStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ReviewEntity r
                set r.status = :status
                where r.parent.id = :parentId
            """)
    int rejectChildReviews(
            @Param("parentId") Long parentId,
            @Param("status") ReviewStatus status
    );

    Optional<ReviewEntity> findFirstBySourceAndPlace_PlaceIdOrderByCreatedAtDesc(
            ReviewSource source,
            String placeId
    );

    boolean existsByPlace_PlaceIdAndSource(String placeId, ReviewSource source);


    @Query("Select r.createdAt from ReviewEntity r Where r.place.placeId = :placeId")
    Set<Long> findAllCreatedAtsByPlaceId(@Param("placeId") String placeId);

    @Query("SELECT r.time FROM ReviewEntity r WHERE r.place.placeId = :placeId AND r.time IS NOT NULL")
    Set<Long> findAllTimesByPlaceId(@Param("placeId") String placeId);

    @Query("SELECT r.time FROM ReviewEntity r WHERE r.place.id = :placeDbId AND r.time IS NOT NULL")
    Set<Long> findAllTimesByPlaceDbId(@Param("placeDbId") Long placeDbId);


}
