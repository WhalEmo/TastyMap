package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.common.CalculateRelationStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import com.beem.TastyMap.userRelated.subscribe.RelationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SocialNotificationService {

    private final SocialNotificationRepo notificationRepo;
    private final CalculateRelationStatus relationStatusCalculator;

    private static final Set<SocialNotificationType> FOLLOW_RELATED_TYPES = Set.of(
            SocialNotificationType.FOLLOW_REQUEST,
            SocialNotificationType.NEW_FOLLOWER,
            SocialNotificationType.FOLLOW_ACCEPTED
    );

    public SocialNotificationService(SocialNotificationRepo notificationRepo, CalculateRelationStatus relationStatusCalculator) {
        this.notificationRepo = notificationRepo;
        this.relationStatusCalculator = relationStatusCalculator;
    }

    @Transactional
    public void createNotification(UserEntity recipient, UserEntity actor, SocialNotificationType type, Long targetId, String content) {
        if (recipient.getId().equals(actor.getId())) return;

        SocialNotificationEntity notification = new SocialNotificationEntity();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTargetId(targetId);
        notification.setContent(content);

        notificationRepo.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<SocialNotificationDTO> getUserNotifications(Long myId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SocialNotificationDTO> rawPage = notificationRepo.findByRecipientId(myId, pageable);
        List<SocialNotificationDTO> updatedList = rawPage.getContent().stream().map(notif -> {
            Long actorId = notif.actor().id();

            RelationStatus status = null;
            if (FOLLOW_RELATED_TYPES.contains(notif.type())) {
                status = relationStatusCalculator.calculate(myId, actorId);
            }

            SocialNotificationDTO.ActorDTO updatedActor = new SocialNotificationDTO.ActorDTO(
                    actorId,
                    notif.actor().username(),
                    notif.actor().profilePhotoUrl(),
                    status
            );

            return new SocialNotificationDTO(
                    notif.id(),
                    notif.type(),
                    notif.actionStatus(),
                    notif.isRead(),
                    notif.createdAt(),
                    updatedActor,
                    notif.target()
            );
        }).toList();

        return new PageImpl<>(updatedList, pageable, rawPage.getTotalElements());
    }

    @Transactional
    public void markNotificationAsRead(Long userId) {
        notificationRepo.markAsRead(userId);
    }

    @Transactional
    public void updateNotification(Long recipientId, Long actorId, SocialNotificationType type, NotificationActionStatus status){
        notificationRepo.updateActionStatus(recipientId,actorId,type,status);
    }
}
