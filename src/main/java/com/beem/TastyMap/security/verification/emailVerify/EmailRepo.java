package com.beem.TastyMap.security.verification.emailVerify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailRepo extends JpaRepository<EmailEntity,Long> {
    Optional<EmailEntity> findByToken(String token);

    boolean existsByUser_IdAndUsedFalseAndExpiryDateAfter(Long userId, LocalDateTime date);

    long countByUserIdAndDeviceIdAndCreatedAtAfter(Long userId, String deviceId, LocalDateTime dateTime);
    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime dateTime);

    boolean existsByDeviceIdAndUsedTrue(String deviceId);
}
