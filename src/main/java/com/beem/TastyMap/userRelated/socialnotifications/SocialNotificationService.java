package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import com.beem.TastyMap.userRelated.subscribe.RelationStatus;
import com.beem.TastyMap.userRelated.subscribe.SubscribeEntity;
import com.beem.TastyMap.userRelated.subscribe.SubscribeRepo;
import com.beem.TastyMap.userRelated.subscribe.SubscribeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SocialNotificationService {

    private final SocialNotificationRepo notificationRepo;
    private final SubscribeRepo subscribeRepo;
    private static final int MAX_NOTIFICATION_LIMIT = 30;

    private static final Set<SocialNotificationType> FOLLOW_RELATED_TYPES = Set.of(
            SocialNotificationType.FOLLOW_REQUEST,
            SocialNotificationType.NEW_FOLLOWER,
            SocialNotificationType.FOLLOW_ACCEPTED
    );
    private static final List<SocialNotificationType> FOLLOW_NOTIFICATION_TYPES = List.of(
            SocialNotificationType.NEW_FOLLOWER,
            SocialNotificationType.FOLLOW_REQUEST,
            SocialNotificationType.FOLLOW_ACCEPTED
    );

    public SocialNotificationService(SocialNotificationRepo notificationRepo, SubscribeRepo subscribeRepo) {
        this.notificationRepo = notificationRepo;
        this.subscribeRepo = subscribeRepo;
    }

    @Transactional
    public void createNotification(UserEntity recipient, NotificationActionStatus status, UserEntity actor, SocialNotificationType type, Long targetId, String content) {
        if (recipient.getId().equals(actor.getId())) return;

        SocialNotificationEntity notification = new SocialNotificationEntity();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTargetId(targetId);
        notification.setActionStatus(status);
        notification.setContent(content);

        notificationRepo.save(notification);
        cleanupOldNotifications(recipient.getId());
    }

    private void cleanupOldNotifications(Long recipientId) {
        notificationRepo.deleteOldNotificationsExcludingTopN(recipientId, MAX_NOTIFICATION_LIMIT);
    }
    @Transactional
    public void clearFollowNotificationsBetween(Long userA, Long userB) {
        notificationRepo.deleteNotificationsBetweenUsers(userA, userB, FOLLOW_NOTIFICATION_TYPES);
    }

    @Transactional
    public void deleteOutgoingFollowNotifications(Long actorId, Long recipientId) {
        List<SocialNotificationType> types = Arrays.asList(
                SocialNotificationType.FOLLOW_REQUEST,
                SocialNotificationType.NEW_FOLLOWER
        );
        notificationRepo.deleteDirectionalFollowNotifications(actorId, recipientId, types);
    }

    @Transactional(readOnly = true)
    public boolean checkHasUnread(Long recipientId) {
        return notificationRepo.existsByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional(readOnly = true)
    public Page<SocialNotificationDTO> getUserNotifications(Long myId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SocialNotificationDTO> rawPage = notificationRepo.findByRecipientId(myId, pageable);

        if (rawPage.isEmpty()) {
            return rawPage;
        }

        Set<Long> followActorIds = new HashSet<>();
        for (SocialNotificationDTO notif : rawPage.getContent()) {
            if (FOLLOW_RELATED_TYPES.contains(notif.type())) {
                followActorIds.add(notif.actor().id());
            }
        }

        Map<Long, RelationStatus> relationStatusMap = computeRelationStatusesInBatch(myId, followActorIds);

        List<SocialNotificationDTO> updatedList = rawPage.getContent().stream().map(notif -> {
            Long actorId = notif.actor().id();
            RelationStatus status = relationStatusMap.get(actorId);

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

    private Map<Long, RelationStatus> computeRelationStatusesInBatch(Long myId, Set<Long> actorIds) {
        if (actorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SubscribeEntity> relations = subscribeRepo.findRelationsBetweenMyIdAndActors(myId, actorIds);

        Map<Long, List<SubscribeEntity>> relationsByActor = new HashMap<>();
        for (SubscribeEntity rel : relations) {
            Long otherId = rel.getSubscriber().getId().equals(myId)
                    ? rel.getSubscribed().getId()
                    : rel.getSubscriber().getId();
            relationsByActor.computeIfAbsent(otherId, k -> new ArrayList<>()).add(rel);
        }

        Map<Long, RelationStatus> result = new HashMap<>();
        for (Long actorId : actorIds) {
            List<SubscribeEntity> actorRelations = relationsByActor.getOrDefault(actorId, Collections.emptyList());
            RelationStatus myStatus = RelationStatus.NOT_FOLLOWING;
            boolean isFollower = false;

            for (SubscribeEntity rel : actorRelations) {
                if (rel.getSubscriber().getId().equals(myId)) {
                    if (rel.getStatus() == SubscribeStatus.ACCEPTED) myStatus = RelationStatus.FOLLOWING;
                    else if (rel.getStatus() == SubscribeStatus.PENDING) myStatus = RelationStatus.PENDING;
                } else {
                    if (rel.getStatus() == SubscribeStatus.ACCEPTED) isFollower = true;
                }
            }

            if (myStatus == RelationStatus.NOT_FOLLOWING && isFollower) {
                myStatus = RelationStatus.FOLLOW_BACK;
            }

            result.put(actorId, myStatus);
        }

        return result;
    }

    @Transactional
    public void markNotificationAsRead(Long userId) {
        notificationRepo.markAsRead(userId);
    }

    @Transactional
    public void updateNotification(Long recipientId, Long actorId, SocialNotificationType type, NotificationActionStatus status){
        notificationRepo.updateActionStatus(recipientId, actorId, type, status);
    }
}