package com.beem.TastyMap.socialnotificaiton.dto;

import com.beem.TastyMap.socialnotificaiton.model.NotificationActionStatus;
import com.beem.TastyMap.socialnotificaiton.model.SocialNotificationType;
import com.beem.TastyMap.user.subscribe.model.RelationStatus;

import java.time.LocalDateTime;

public record SocialNotificationDTO(
        Long id,
        SocialNotificationType type,
        NotificationActionStatus actionStatus,
        boolean isRead,
        LocalDateTime createdAt,
        ActorDTO actor,
        TargetDTO target
) {
    public record ActorDTO(
            Long id,
            String username,
            String profilePhotoUrl,
            RelationStatus relationStatus
    ) {}

    public record TargetDTO(
            Long targetId,
            String mediaUrl, // Postun küçük resmi (beğeni/yorum ise)
            String content   // Yorum ise yorumun kendisi
    ) {}
}
