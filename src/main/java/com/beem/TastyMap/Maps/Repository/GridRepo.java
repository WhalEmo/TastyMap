package com.beem.TastyMap.Maps.Repository;

import com.beem.TastyMap.Maps.Entity.GridEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridRepo extends JpaRepository<GridEntity, Long> {

    @Query(
            value = """
        select DISTINCT g.* from grids g
            JOIN places p on p.grid_id = g.id
            JOIN place_types pt on pt.place_id = p.id
                where g.center_lat = :gridLat
                AND g.center_lng = :gridLng
                AND pt.type in (:types)
        """,
            nativeQuery = true
    )
    Optional<GridEntity> findByGridAndTypes(
            @Param("gridLat") BigDecimal gridLat,
            @Param("gridLng") BigDecimal gridLng,
            @Param("types") List<String> types
    );
}
