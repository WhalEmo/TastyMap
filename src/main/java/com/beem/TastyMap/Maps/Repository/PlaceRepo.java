package com.beem.TastyMap.Maps.Repository;


import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

}
