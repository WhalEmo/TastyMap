package com.beem.TastyMap.maps.repository;

import com.beem.TastyMap.maps.entity.PhotoEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PhotoRepo extends JpaRepository<PhotoEntity, Long> {

    @Query("Select p.photoReference from PhotoEntity p Where p.place.placeId = :placeId")
    Set<String> findAllReferencesByPlaceId(@Param("placeId") String placeId);
}
