package com.beem.TastyMap.Maps.Repository;

import com.beem.TastyMap.Maps.Entity.PhotoEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PhotoRepo extends JpaRepository<PhotoEntity, Long> {

    @Query("Select p.photoReference from PhotoEntity p Where p.place.placeId = :placeId")
    Set<String> findAllReferencesByPlaceId(@Param("placeId") String placeId);
}
