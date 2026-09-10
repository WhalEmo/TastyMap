package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialNotificationRepo extends JpaRepository<SocialNotificationEntity, Long>, SocialNotificationRepoCustom {
    boolean existsByRecipientIdAndIsReadFalse(Long recipientId);


    @Modifying
    @Query("DELETE FROM SocialNotificationEntity n WHERE " +
            "n.actor.id = :actorId AND n.recipient.id = :recipientId " +
            "AND n.type IN :types")
    void deleteDirectionalFollowNotifications(
            @Param("actorId") Long actorId,
            @Param("recipientId") Long recipientId,
            @Param("types") List<SocialNotificationType> types
    );

    @Modifying
    @Query("DELETE FROM SocialNotificationEntity n WHERE " +
            "((n.recipient.id = :userA AND n.actor.id = :userB) OR (n.recipient.id = :userB AND n.actor.id = :userA)) " +
            "AND n.type IN :types")
    void deleteNotificationsBetweenUsers(
            @Param("userA") Long userA,
            @Param("userB") Long userB,
            @Param("types") List<SocialNotificationType> types
    );


    @Modifying
    @Query(value = """
        DELETE FROM social_notifications
        WHERE recipient_id = :recipientId
          AND id NOT IN (
            SELECT id FROM (
              SELECT id FROM social_notifications
              WHERE recipient_id = :recipientId
              ORDER BY created_at DESC
              LIMIT :limit
            ) AS keep_ids
          )
        """, nativeQuery = true)
    void deleteOldNotificationsExcludingTopN(@Param("recipientId") Long recipientId, @Param("limit") int limit);
}
