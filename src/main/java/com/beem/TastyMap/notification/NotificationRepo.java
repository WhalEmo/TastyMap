package com.beem.TastyMap.notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepo extends JpaRepository<NotificationEntity,Long> {
    Optional<NotificationEntity> findByUser_IdAndDeviceId(Long userId, String deviceId);
    @Query("SELECT n FROM NotificationEntity n JOIN FETCH n.user WHERE n.deviceId = :deviceId AND n.status = :status")
    Optional<NotificationEntity> findByDeviceIdAndStatusWithUser(@Param("deviceId") String deviceId, @Param("status") Status status);

    @Query("SELECT n FROM NotificationEntity n JOIN FETCH n.user WHERE n.token = :token")
    Optional<NotificationEntity> findByTokenWithUser(@Param("token") String token);

    boolean existsByUser_IdAndDeviceIdAndStatus(Long userId, String deviceId, Status status);
    @Query("SELECT n.user.id FROM NotificationEntity n WHERE n.id = :id")
    Optional<Long> findUserIdById(@Param("id") Long id);
}
