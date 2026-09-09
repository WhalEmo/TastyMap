package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialNotificationRepo extends JpaRepository<SocialNotificationEntity, Long>, SocialNotificationRepoCustom {
    boolean existsByRecipientIdAndIsReadFalse(Long recipientId);
    void deleteByRecipient_IdAndActor_IdAndType(Long recipientId, Long actorId, SocialNotificationType type);

    @Query("SELECT n.id FROM SocialNotificationEntity n WHERE n.recipient.id = :recipientId ORDER BY n.createdAt DESC")
    List<Long> findIdsByRecipientIdOrderByCreatedAtDesc(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("DELETE FROM SocialNotificationEntity n WHERE n.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);
}
