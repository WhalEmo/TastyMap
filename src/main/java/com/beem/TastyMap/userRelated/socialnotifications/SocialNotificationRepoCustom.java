package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SocialNotificationRepoCustom {
    Page<SocialNotificationDTO> findByRecipientId(Long recipientId, Pageable pageable);
    void markAsRead(Long recipientId);
    void updateActionStatus(Long recipientId, Long actorId, SocialNotificationType type, NotificationActionStatus status);
}
