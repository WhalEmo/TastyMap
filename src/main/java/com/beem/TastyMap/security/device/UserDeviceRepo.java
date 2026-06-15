package com.beem.TastyMap.security.device;

import com.beem.TastyMap.security.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepo extends JpaRepository<UserDeviceEntity,Long> {
    Optional<UserDeviceEntity>findByUser_IdAndDeviceId(Long userId, String deviceId);
    List<UserDeviceEntity> findByUser_IdAndIsTrustedTrue(Long userId);
    long countByUser_IdAndIsTrustedTrue(Long userId);
    List<UserDeviceEntity>findByUser_Id(Long userId);
}
