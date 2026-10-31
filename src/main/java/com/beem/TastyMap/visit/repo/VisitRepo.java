package com.beem.TastyMap.visit.repo;

import com.beem.TastyMap.visit.entity.VisitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepo extends JpaRepository<VisitEntity,Long> ,VisitRepoCustom{
    boolean existsByUserIdAndPlaceEmbeddedPlaceIdAndIsDeleteFalse(Long userId, String placeId);
}
