package com.beem.TastyMap.security.verification.forgotPassword;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordRepo extends JpaRepository<PasswordEntity,Long> {
    Optional<PasswordEntity>findByToken(String token);
    boolean existsByUser_IdAndUsedFalseAndExpiryDateAfter(Long userId, LocalDateTime now);
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime time);
}
