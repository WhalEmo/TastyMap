package com.beem.TastyMap.Security.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepo extends JpaRepository<NotificationEntity,Long> {
    Optional<NotificationEntity>findByUserIdAndDeviceId(Long userId, String deviceId);
    boolean existsByUserIdAndDeviceIdAndStatus(Long userId, String deviceId,Status status);
    Optional<NotificationEntity>findByUserIdAndDeviceIdAndStatus(Long userid, String deviceId,Status status);
}
