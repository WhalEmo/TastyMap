package com.beem.TastyMap.rag.repository;

import com.beem.TastyMap.rag.entity.UserAiQuotaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAiQuotaRepository extends JpaRepository<UserAiQuotaEntity, Long> {

    // Eşzamanlı isteklerde kotanın eksiye düşmemesi için kilitli okuma
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM UserAiQuotaEntity q WHERE q.userId = :userId")
    Optional<UserAiQuotaEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}