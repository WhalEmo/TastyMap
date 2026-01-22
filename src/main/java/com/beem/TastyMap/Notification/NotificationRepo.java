package com.beem.TastyMap.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepo extends JpaRepository<NotificationEntity,Long> {
    Optional<NotificationEntity>findByUserIdAndDeviceId(Long userId, String deviceId);
    Optional<NotificationEntity>findByUserIdAndDeviceIdAndStatus(Long userid, String deviceId,Status status);
}
