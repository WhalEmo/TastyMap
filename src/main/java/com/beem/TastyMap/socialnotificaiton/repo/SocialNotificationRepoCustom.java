package com.beem.TastyMap.socialnotificaiton.repo;

import com.beem.TastyMap.socialnotificaiton.dto.SocialNotificationDTO;
import com.beem.TastyMap.socialnotificaiton.model.NotificationActionStatus;
import com.beem.TastyMap.socialnotificaiton.model.SocialNotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SocialNotificationRepoCustom {
    Page<SocialNotificationDTO> findByRecipientId(Long recipientId, Pageable pageable);
    void markAsRead(Long recipientId);
    void updateActionStatus(Long recipientId, Long actorId, SocialNotificationType type, NotificationActionStatus status);
}
