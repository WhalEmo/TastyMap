package com.beem.TastyMap.userRelated.profile;


import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import com.beem.TastyMap.userRelated.subscribe.RelationStatus;
import com.beem.TastyMap.userRelated.subscribe.SubscribeEntity;
import com.beem.TastyMap.userRelated.subscribe.SubscribeRepo;
import com.beem.TastyMap.userRelated.subscribe.SubscribeStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OtherProfileService {

    private final UserRepo userRepo;
    private final BlockRepo blockRepo;
    private final SubscribeRepo subscribeRepo;
    private final MessageSource messageSource;

    public OtherProfileService(UserRepo userRepo,
                               BlockRepo blockRepo,
                               SubscribeRepo subscribeRepo,
                               MessageSource messageSource) {
        this.userRepo = userRepo;
        this.blockRepo = blockRepo;
        this.subscribeRepo = subscribeRepo;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional(readOnly = true)
    public ProfileDTOresponse getUserProfile(Long targetUserId, Long myId) {
        if (targetUserId.equals(myId)) {
            UserEntity user = userRepo.findById(myId)
                    .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found.simple")));

            return new ProfileDTOresponse(
                    user.getUsername(), user.getName(), user.getSurname(),
                    user.getProfile(), user.getRole(), user.getBiography(),
                    user.getPostCount(), user.getSubscriberCount(), user.getSubscribedCount(),
                    false, false, RelationStatus.SELF,false
            );
        }

        UserEntity user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found.simple")));

        boolean blockedByMe = blockRepo.existsByBlocker_IdAndBlocked_Id(myId, targetUserId);
        boolean blockedMe = blockRepo.existsByBlocker_IdAndBlocked_Id(targetUserId, myId);

        if (blockedByMe || blockedMe) {
            return new ProfileDTOresponse(
                    user.getUsername(), user.getName(), user.getSurname(),
                    null, user.getRole(), user.getBiography(),
                    0, 0, 0,
                    blockedByMe, blockedMe, RelationStatus.NOT_FOLLOWING,false
            );
        }
        // 1. Sizin karşı tarafa olan durumunuz (FOLLOWING, PENDING, FOLLOW_BACK veya NOT_FOLLOWING)
        RelationStatus myStatus = calculateRelationStatus(myId, targetUserId);

        // 2. Karşı tarafın size attığı onay bekleyen istek var mı?
        boolean hasPendingIncoming = subscribeRepo.existsBySubscriber_IdAndSubscribed_IdAndStatus(
                targetUserId, myId, SubscribeStatus.PENDING
        );

        return new ProfileDTOresponse(
                user.getUsername(), user.getName(), user.getSurname(),
                user.getProfile(), user.getRole(), user.getBiography(),
                user.getPostCount(), user.getSubscriberCount(), user.getSubscribedCount(),
                false, false, myStatus, hasPendingIncoming
        );

    }

    private RelationStatus calculateRelationStatus(Long myId, Long targetUserId) {
        // Bizim karşı tarafa attığımız isteğin durumu (Sadece status çekilir)
        Optional<SubscribeStatus> myRequestStatus = subscribeRepo.findStatusBySubscriberIdAndSubscribedId(myId, targetUserId);

        if (myRequestStatus.isPresent()) {
            return myRequestStatus.get() == SubscribeStatus.ACCEPTED
                    ? RelationStatus.FOLLOWING
                    : RelationStatus.PENDING;
        }

        // Karşı taraf bizi takip ediyor mu?
        boolean isFollower = subscribeRepo.existsBySubscriber_IdAndSubscribed_IdAndStatus(
                targetUserId, myId, SubscribeStatus.ACCEPTED
        );

        return isFollower ? RelationStatus.FOLLOW_BACK : RelationStatus.NOT_FOLLOWING;
    }
}
