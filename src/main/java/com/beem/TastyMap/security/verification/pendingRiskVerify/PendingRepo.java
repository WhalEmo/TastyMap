package com.beem.TastyMap.security.verification.pendingRiskVerify;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PendingRepo extends JpaRepository<PendingEntity,Long> {
    @Query("SELECT p FROM PendingEntity p JOIN FETCH p.user WHERE p.token = :token")
    Optional<PendingEntity> findByTokenWithUser(@Param("token") String token);
}
