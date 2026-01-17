package com.beem.TastyMap.Security;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity,Long> {
    Optional<RefreshTokenEntity>findByTokenAndRevokedFalse(String token);
    Optional<RefreshTokenEntity>findByUserIdAndDeviceIdAndRevokedFalse(Long userid,String DeviceId);
    boolean existsByUserIdAndDeviceIdAndRevokedFalse(Long userid,String DeviceId);

}
