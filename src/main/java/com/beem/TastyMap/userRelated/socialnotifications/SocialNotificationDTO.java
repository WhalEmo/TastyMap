package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import com.beem.TastyMap.userRelated.subscribe.RelationStatus;

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
