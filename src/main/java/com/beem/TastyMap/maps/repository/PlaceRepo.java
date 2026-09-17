package com.beem.TastyMap.maps.repository;


import com.beem.TastyMap.maps.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PlaceRepo extends JpaRepository<PlaceEntity, Long> {



    @Query(value = """
            Select p.placeId, p.id from PlaceEntity p
                Where p.placeId IN :placeIds
            """
    )
    List<Object[]> findIdsByPlaceIds(@Param("placeIds")Set<String> placeIds);

    Optional<PlaceEntity> findByPlaceId(String placeId);


    @Query("""
        SELECT DISTINCT p FROM PlaceEntity p
        LEFT JOIN FETCH p.types
        JOIN p.grid g
        WHERE g.centerLat = :gridLat
          AND g.centerLng = :gridLng
          AND EXISTS (
              SELECT 1 FROM p.types t WHERE t IN :types
          )
    """)
    List<PlaceEntity> findPlacesByGridAndTypes(
            @Param("gridLat") BigDecimal gridLat,
            @Param("gridLng") BigDecimal gridLng,
            @Param("types") List<String> types
    );

}
