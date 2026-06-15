package com.beem.TastyMap.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity,Long> {
    Optional<RefreshTokenEntity> findByUser_IdAndDeviceIdAndRevokedFalse(Long userId, String deviceId);
    boolean existsByUser_IdAndDeviceIdAndRevokedFalse(Long userId, String deviceId);
    @Query("SELECT r FROM RefreshTokenEntity r JOIN FETCH r.user WHERE r.token = :token AND r.revoked = false")
    Optional<RefreshTokenEntity> findByTokenWithUser(@Param("token") String token);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE RefreshTokenEntity r
        SET r.revoked = true
        WHERE r.user.id = :userId
          AND r.revoked = false
    """)
    int revokeAllByUser(@Param("userId") Long userId);
}
