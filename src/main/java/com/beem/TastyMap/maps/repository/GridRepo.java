package com.beem.TastyMap.maps.repository;

import com.beem.TastyMap.maps.entity.GridEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridRepo extends JpaRepository<GridEntity, Long> {

    @Query(value = """
            select g.* from grids g
                where g.center_lat = :gridLat
                And g.center_lng = :gridLng
            """,
            nativeQuery = true
    )
    Optional<GridEntity> findByGridLatAndLng(
            @Param("gridLat") BigDecimal gridLat,
            @Param("gridLng") BigDecimal gridLng
    );

    @Query(value = """
            select g.id from grids g
            where g.center_lat = :gridLat
                And g.center_lng = :gridLng
            """,
            nativeQuery = true
    )
    Optional<Long> findIdByGridLatAndLng(
            @Param("gridLat") BigDecimal gridLat,
            @Param("gridLng") BigDecimal gridLng
    );
}
