package com.beem.TastyMap.security.banned;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BannedDeviceRepo extends JpaRepository<BannedDeviceEntity, Long> {
    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<BannedDeviceEntity>findByUser_IdAndDeviceId(Long userId,String deviceId);
}