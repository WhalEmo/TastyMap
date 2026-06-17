package com.beem.TastyMap.notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepo extends JpaRepository<NotificationEntity,Long> {
    Optional<NotificationEntity> findByUser_IdAndDeviceId(Long userId, String deviceId);
    boolean existsByUser_IdAndDeviceIdAndStatus(Long userId, String deviceId, Status status);
    @Query("SELECT n.user.id FROM NotificationEntity n WHERE n.id = :id")
    Optional<Long> findUserIdById(@Param("id") Long id);
}
