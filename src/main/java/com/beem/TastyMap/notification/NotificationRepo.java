package com.beem.TastyMap.notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepo extends JpaRepository<NotificationEntity,Long> {
    @EntityGraph(attributePaths = {"user"})
    Optional<NotificationEntity> findFirstByDeviceIdAndIsUsedFalseOrderByCreatedAtDesc(String deviceId);

    @EntityGraph(attributePaths = {"user"})
    Optional<NotificationEntity> findFirstByDeviceIdAndIsUsedTrueOrderByCreatedAtDesc(String deviceId);

    @Query("SELECT n FROM NotificationEntity n JOIN FETCH n.user WHERE n.token = :token")
    Optional<NotificationEntity> findByTokenWithUser(@Param("token") String token);

    //boolean existsByUser_IdAndDeviceIdAndStatus(Long userId, String deviceId, Status status);
    @Query("SELECT COUNT(n) > 0 FROM NotificationEntity n " +
            "WHERE n.user.id = :userId " +
            "AND n.deviceId = :deviceId " +
            "AND n.status = :status " +
            "AND n.expiresAt > CURRENT_TIMESTAMP")
    boolean existsActiveNotification(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("status") Status status
    );

    @Query("SELECT n.user.id FROM NotificationEntity n WHERE n.id = :id")
    Optional<Long> findUserIdById(@Param("id") Long id);

    boolean existsByDeviceIdAndCreatedAtAfter(String deviceId, LocalDateTime after);

    List<NotificationEntity> findAllByDeviceIdAndCreatedAtAfterOrderByCreatedAtDesc(String deviceId, LocalDateTime after);

}
