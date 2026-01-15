package com.beem.TastyMap.Maps.Repository;


import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PlaceRepo extends JpaRepository<PlaceEntity, Long> {

    @Query(
            value = """
        SELECT p.*
            FROM places p
                WHERE p.grid_lat = :gridLat
                  AND p.grid_lng = :gridLng
                  AND EXISTS (
                      SELECT 1
                      FROM place_types pt
                      WHERE pt.place_id = p.id
                        AND pt.type IN (:types)
                  )
        """,
            nativeQuery = true
    )
    List<PlaceEntity> findByGridAndTypes(
            @Param("gridLat") BigDecimal gridLat,
            @Param("gridLng") BigDecimal gridLng,
            @Param("types") List<String> types
    );
}
