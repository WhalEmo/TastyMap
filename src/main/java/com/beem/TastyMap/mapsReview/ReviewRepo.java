package com.beem.TastyMap.mapsReview;

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

    Page<ReviewEntity> findByPlace_PlaceId(String placeId, Pageable pageable);

    @Query("SELECT r FROM ReviewEntity r LEFT JOIN FETCH r.user WHERE r.place.placeId = :placeId")
    List<ReviewEntity> findAllByPlaceId(@Param("placeId") String placeId, Pageable pageable);

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


}
