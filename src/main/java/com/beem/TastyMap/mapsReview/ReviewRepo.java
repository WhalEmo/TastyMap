package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.mapsReview.Enum.ReviewSource;
import com.beem.TastyMap.mapsReview.Enum.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepo extends JpaRepository<ReviewEntity, Long> {

    Page<ReviewEntity> findByPlace_PlaceId(String placeId, Pageable pageable);

    boolean existsByIdAndPlaceId(Long id, Long placeId);

    boolean existsByIdAndPlaceIdAndSourceAndStatus(
            Long id,
            Long placeId,
            ReviewSource source,
            ReviewStatus status
    );

}
