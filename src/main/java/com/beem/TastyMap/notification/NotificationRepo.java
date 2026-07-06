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


    @Query("SELECT n.status as status, n.isUsed as isUsed " +
            "FROM NotificationEntity n " +
            "WHERE n.deviceId = :deviceId AND n.isUsed = true " +
            "ORDER BY n.createdAt DESC LIMIT 1")
    Optional<NotificationStatusSummary> findLatestNotificationStatus(@Param("deviceId") String deviceId);



    @Query("SELECT n FROM NotificationEntity n JOIN FETCH n.user WHERE n.token = :token")
    Optional<NotificationEntity> findByTokenWithUser(@Param("token") String token);


    @Query("SELECT COUNT(n) > 0 FROM NotificationEntity n " +
            "WHERE n.user.id = :userId " +
            "AND n.status = :status " +
            "AND n.expiresAt > CURRENT_TIMESTAMP")
    boolean existsActiveNotificationForUser(
            @Param("userId") Long userId,
            @Param("status") Status status
    );

    @Query("SELECT n.user.id FROM NotificationEntity n WHERE n.id = :id")
    Optional<Long> findUserIdById(@Param("id") Long id);

    boolean existsByDeviceIdAndCreatedAtAfter(String deviceId, LocalDateTime after);

    @Query("SELECT " +
            "SUM(CASE WHEN n.lastIpAddress = :ip THEN 1 ELSE 0 END) as ipAttackCount, " +
            "SUM(CASE WHEN n.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectCount, " +
            "SUM(CASE WHEN n.status IN ('PENDING', 'EXPIRED') THEN 1 ELSE 0 END) as pendingCount " +
            "FROM NotificationEntity n WHERE n.deviceId = :deviceId AND n.createdAt > :timeLimit")
    SecurityHistorySummary getSecurityHistorySummary(
            @Param("deviceId") String deviceId,
            @Param("ip") String ip,
            @Param("timeLimit") LocalDateTime timeLimit
    );

    @Query("SELECT n.updatedAt FROM NotificationEntity n WHERE n.deviceId = :deviceId AND n.status = 'REJECTED' ORDER BY n.createdAt DESC LIMIT 1")
    Optional<LocalDateTime> findLastRejectedTime(@Param("deviceId") String deviceId);


}