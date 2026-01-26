package com.beem.TastyMap.MapsReview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepo extends JpaRepository<ReviewEntity, Long> {

    Page<ReviewEntity> findByPlace_PlaceId(String placeId, Pageable pageable);

}
