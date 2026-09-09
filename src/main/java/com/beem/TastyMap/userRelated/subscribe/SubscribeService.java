package com.beem.TastyMap.userRelated.subscribe;

import com.beem.TastyMap.event.model.FcmNotificationEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import com.beem.TastyMap.userRelated.post.AccessChecker;
import com.beem.TastyMap.userRelated.socialnotifications.SocialNotificationService;
import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscribeService {
    private final UserRepo userRepo;
    private final SubscribeRepo subscribeRepo;
    private final SocialNotificationService socialNotificationService;
    private final AccessChecker accessChecker;
    private final EntityManager entityManager;
    private final BlockRepo blockRepo;
    private final MessageSource messageSource;
    private final ApplicationEventPublisher eventPublisher;


    public SubscribeService(UserRepo userRepo, SubscribeRepo subscribeRepo, SocialNotificationService socialNotificationService, AccessChecker accessChecker, EntityManager entityManager, BlockRepo blockRepo, MessageSource messageSource, ApplicationEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.subscribeRepo = subscribeRepo;
        this.socialNotificationService = socialNotificationService;
        this.accessChecker = accessChecker;
        this.entityManager = entityManager;
        this.blockRepo = blockRepo;
        this.messageSource = messageSource;
        this.eventPublisher = eventPublisher;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    // Takip Et / İstek Gönder
    @Transactional
    public SubscribeActionResult subscribe(Long subscribes, Long myId) {
        if (myId.equals(subscribes)) {
            throw new CustomExceptions.InvalidException(getMessage("subscribe.self.not.allowed"));
        }
        boolean blocked = blockRepo.existsByBlocker_IdAndBlocked_Id(subscribes, myId) ||
                blockRepo.existsByBlocker_IdAndBlocked_Id(myId, subscribes);

        if (blocked) {
            throw new CustomExceptions.ForbiddenException(getMessage("subscribe.blocked"));
        }
        if (subscribeRepo.existsBySubscriber_IdAndSubscribed_Id(myId, subscribes)) {
            throw new CustomExceptions.UserAlreadyExistsException(getMessage("subscribe.already.subscribed"));
        }

        boolean isPrivate = userRepo.isProfilePrivate(subscribes)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found")));

        UserEntity subscriberRef = entityManager.getReference(UserEntity.class, myId);
        UserEntity subscribedRef = entityManager.getReference(UserEntity.class, subscribes);

        SubscribeEntity entity = new SubscribeEntity();
        entity.setSubscriber(subscriberRef);
        entity.setSubscribed(subscribedRef);
        entity.setDate(LocalDateTime.now());

        if (isPrivate) {
            entity.setStatus(SubscribeStatus.PENDING);
            subscribeRepo.save(entity);
            socialNotificationService.createNotification(
                    subscribedRef,
                    NotificationActionStatus.PENDING,
                    subscriberRef,
                    SocialNotificationType.FOLLOW_REQUEST,
                    null,
                    null
            );
            sendFollowNotification(subscribes, subscriberRef.getUsername(), true, myId);
        } else {
            entity.setStatus(SubscribeStatus.ACCEPTED);
            subscribeRepo.save(entity);
            userRepo.updateSubscribedCount(myId, 1);
            userRepo.updateSubscriberCount(subscribes, 1);
            socialNotificationService.createNotification(
                    subscribedRef,
                    NotificationActionStatus.NONE,
                    subscriberRef,
                    SocialNotificationType.NEW_FOLLOWER,
                    null,
                    null
            );
            sendFollowNotification(subscribes, subscriberRef.getUsername(), false, myId);
        }

        return buildActionResult(myId, subscribes);
    }

    // Gelen İstek Kabul Edildiğinde
    @Transactional
    public SubscribeActionResult acceptSubscribeRequest(Long requesterId, Long myId) {
        SubscribeEntity entity = subscribeRepo.findBySubscriber_IdAndSubscribed_Id(requesterId, myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("subscribe.not.found")));

        if (entity.getStatus() == SubscribeStatus.PENDING) {
            entity.setStatus(SubscribeStatus.ACCEPTED);
            subscribeRepo.save(entity);

            userRepo.updateSubscribedCount(requesterId, 1);
            userRepo.updateSubscriberCount(myId, 1);

            socialNotificationService.updateNotification(
                    myId,
                    requesterId,
                    SocialNotificationType.FOLLOW_REQUEST,
                    NotificationActionStatus.ACCEPTED
            );
            UserEntity requesterRef = entityManager.getReference(UserEntity.class, requesterId);
            UserEntity myRef = entityManager.getReference(UserEntity.class, myId);

            socialNotificationService.createNotification(
                    requesterRef,
                    NotificationActionStatus.NONE,
                    myRef,
                    SocialNotificationType.FOLLOW_ACCEPTED,
                    null,
                    null
            );
            sendAcceptNotification(requesterId, myRef.getUsername(), myId);
        }

        return buildActionResult(myId, requesterId);
    }

    // Gelen İstek Reddedildiğinde
    @Transactional
    public SubscribeActionResult rejectSubscribeRequest(Long requesterId, Long myId) {
        SubscribeEntity entity = subscribeRepo.findBySubscriber_IdAndSubscribed_Id(requesterId, myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("subscribe.not.found")));

        if (entity.getStatus() == SubscribeStatus.PENDING) {
            subscribeRepo.delete(entity);
        }

        socialNotificationService.updateNotification(
                myId,
                requesterId,
                SocialNotificationType.FOLLOW_REQUEST,
                NotificationActionStatus.REJECTED
        );
        return buildActionResult(myId, requesterId);
    }

    // Takipten Çıkma / İstek İptal Etme
    @Transactional
    public SubscribeActionResult unSubscribe(Long subscribes, Long myId) {
        SubscribeEntity entity = subscribeRepo.findBySubscriber_IdAndSubscribed_Id(myId, subscribes)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("subscribe.not.found")));

        boolean wasAccepted = entity.getStatus() == SubscribeStatus.ACCEPTED;
        subscribeRepo.delete(entity);

        if (wasAccepted) {
            userRepo.updateSubscribedCount(myId, -1);
            userRepo.updateSubscriberCount(subscribes, -1);
        }else {
            socialNotificationService.deleteNotification(subscribes, myId, SocialNotificationType.FOLLOW_REQUEST);
        }

        return buildActionResult(myId, subscribes);
    }

    // Aboneyi (Takipçiyi) Çıkarma
    @Transactional
    public SubscribeActionResult unSubscriber(Long subscribes, Long myId) {
        SubscribeEntity entity = subscribeRepo.findBySubscriber_IdAndSubscribed_Id(subscribes, myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("subscribe.not.found")));

        boolean wasAccepted = entity.getStatus() == SubscribeStatus.ACCEPTED;
        subscribeRepo.delete(entity);

        if (wasAccepted) {
            userRepo.updateSubscribedCount(subscribes, -1);
            userRepo.updateSubscriberCount(myId, -1);
        }

        return buildActionResult(myId, subscribes);
    }

    // Tarafımıza düşen durumları toplayıp mobilin işleyebileceği net durumu veren hesaplama
    private SubscribeActionResult buildActionResult(Long myId, Long targetUserId) {
        Optional<SubscribeStatus> myRequestStatus = subscribeRepo.findStatusBySubscriberIdAndSubscribedId(myId, targetUserId);

        RelationStatus relationStatus;
        if (myRequestStatus.isPresent()) {
            relationStatus = myRequestStatus.get() == SubscribeStatus.ACCEPTED
                    ? RelationStatus.FOLLOWING
                    : RelationStatus.PENDING;
        } else {
            boolean isFollower = subscribeRepo.existsBySubscriber_IdAndSubscribed_IdAndStatus(
                    targetUserId, myId, SubscribeStatus.ACCEPTED
            );
            relationStatus = isFollower ? RelationStatus.FOLLOW_BACK : RelationStatus.NOT_FOLLOWING;
        }

        boolean hasPendingIncoming = subscribeRepo.existsBySubscriber_IdAndSubscribed_IdAndStatus(
                targetUserId, myId, SubscribeStatus.PENDING
        );


        return new SubscribeActionResult(
                targetUserId,
                relationStatus,
                hasPendingIncoming
        );
    }

    public Page<SubscribeDTO> getUserSubscribes(Long userId, Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        accessChecker.checkAccess(userId, myId);
        return subscribeRepo.findUserSubscribes(userId, myId, pageable);
    }

    public Page<SubscribeDTO> getUserSubscribers(Long userId, Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        accessChecker.checkAccess(userId, myId);
        return subscribeRepo.findUserSubscribers(userId, myId, pageable);
    }

    public Page<SubscribeDTO> getPendingRequests(Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        return subscribeRepo.findPendingRequests(myId, pageable);
    }

    private void sendFollowNotification(Long targetUserId, String senderUsername, boolean isPending, Long senderId) {
        String titleCode = isPending ? "notification.follow.request.title" : "notification.follow.accept.title";
        String bodyCode = isPending ? "notification.follow.request.body" : "notification.follow.accept.body";

        Map<String, String> payloadData = Map.of(
                "type", isPending ? "FOLLOW_REQUEST" : "NEW_FOLLOWER",
                "userId", senderId.toString()
        );

        FcmNotificationEvent event = new FcmNotificationEvent(
                targetUserId,
                titleCode,
                bodyCode,
                new Object[]{senderUsername},
                payloadData
        );

        eventPublisher.publishEvent(event);
    }

    private void sendAcceptNotification(Long targetUserId, String senderUsername, Long senderId) {
        Map<String, String> payloadData = Map.of(
                "type", "FOLLOW_ACCEPTED",
                "userId", senderId.toString()
        );

        FcmNotificationEvent event = new FcmNotificationEvent(
                targetUserId,
                "notification.follow.request.accepted.title", // "Takip İsteği Kabul Edildi"
                "notification.follow.request.accepted.body",  // "X takip isteğinizi kabul etti."
                new Object[]{senderUsername},
                payloadData
        );

        eventPublisher.publishEvent(event);
    }
}