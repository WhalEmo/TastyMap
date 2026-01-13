package com.beem.TastyMap.Maps.Repository;


import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceRepo extends JpaRepository<PlaceEntity, Long> {
}
