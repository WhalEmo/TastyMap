package com.beem.TastyMap.security.verification.emailVerify;

import com.beem.TastyMap.registerLogin.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailRepo extends JpaRepository<EmailEntitiy,Long> {
    Optional<EmailEntitiy> findByToken(String token);

    boolean existsByUser_IdAndUsedFalseAndExpiryDateAfter(Long userId, LocalDateTime date);

    long countByUserIdAndDeviceIdAndCreatedAtAfter(Long userId, String deviceId, LocalDateTime dateTime);
    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime dateTime);
}
