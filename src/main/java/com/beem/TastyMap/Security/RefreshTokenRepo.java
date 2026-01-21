package com.beem.TastyMap.Security;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity,Long> {
    Optional<RefreshTokenEntity>findByTokenAndRevokedFalse(String token);
    Optional<RefreshTokenEntity>findByUserIdAndDeviceIdAndRevokedFalse(Long userid,String DeviceId);
    boolean existsByUserIdAndDeviceIdAndRevokedFalse(Long userid,String DeviceId);
    List<RefreshTokenEntity> findAllByUserIdAndRevokedFalse(Long userId);
    long countByUserIdAndRevokedFalse(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE RefreshTokenEntity r
        SET r.revoked = true
        WHERE r.userId = :userId
          AND r.revoked = false
    """)
    int revokeAllByUser(@Param("userId") Long userId);
}
