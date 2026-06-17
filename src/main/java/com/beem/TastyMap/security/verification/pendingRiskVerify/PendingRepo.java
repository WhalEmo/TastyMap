package com.beem.TastyMap.security.verification.pendingRiskVerify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingRepo extends JpaRepository<PendingEntity,Long> {
    Optional<PendingEntity> findByToken(String token);
}
