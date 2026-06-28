package com.beem.TastyMap.security.banned;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BannedDeviceRepo extends JpaRepository<BannedDeviceEntity, Long> {
    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);

    void deleteByUserId(Long userId);
}