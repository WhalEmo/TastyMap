package com.beem.TastyMap.user.profile.service;


import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.user.account.entity.UserEntity;
import com.beem.TastyMap.user.account.repo.UserRepo;
import com.beem.TastyMap.user.block.repo.BlockRepo;
import com.beem.TastyMap.user.profile.dto.ProfileDTOresponse;
import com.beem.TastyMap.user.subscribe.model.RelationStatus;
import com.beem.TastyMap.user.subscribe.entity.SubscribeEntity;
import com.beem.TastyMap.user.subscribe.repo.SubscribeRepo;
import com.beem.TastyMap.user.subscribe.model.SubscribeStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                    false, false, RelationStatus.SELF, false, false,user.isPrivateProfile()
            );
        }

        UserEntity user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found.simple")));

        List<Long> blockerIds = blockRepo.findBlockerIdsBetween(myId, targetUserId);
        boolean blockedByMe = blockerIds.contains(myId);
        boolean blockedMe = blockerIds.contains(targetUserId);

        if (blockedByMe || blockedMe) {
            return new ProfileDTOresponse(
                    user.getUsername(), user.getName(), user.getSurname(),
                    null, user.getRole(), user.getBiography(),
                    0, 0, 0,
                    blockedByMe, blockedMe, RelationStatus.NOT_FOLLOWING, false,false,user.isPrivateProfile()
            );
        }
        List<SubscribeEntity> relations = subscribeRepo.findRelationsBetween(myId, targetUserId);

        RelationStatus myStatus = RelationStatus.NOT_FOLLOWING;
        boolean hasPendingIncoming = false;
        boolean isFollower = false;

        for (SubscribeEntity relation : relations) {
            if (relation.getSubscriber().getId().equals(myId)) {
                if (relation.getStatus() == SubscribeStatus.ACCEPTED) {
                    myStatus = RelationStatus.FOLLOWING;
                } else if (relation.getStatus() == SubscribeStatus.PENDING) {
                    myStatus = RelationStatus.PENDING;
                }
            }
            else if (relation.getSubscriber().getId().equals(targetUserId)) {
                if (relation.getStatus() == SubscribeStatus.ACCEPTED) {
                    isFollower = true;
                } else if (relation.getStatus() == SubscribeStatus.PENDING) {
                    hasPendingIncoming = true;
                }
            }
        }

        if (myStatus == RelationStatus.NOT_FOLLOWING && isFollower) {
            myStatus = RelationStatus.FOLLOW_BACK;
        }

        return new ProfileDTOresponse(
                user.getUsername(), user.getName(), user.getSurname(),
                user.getProfile(), user.getRole(), user.getBiography(),
                user.getPostCount(), user.getSubscriberCount(), user.getSubscribedCount(),
                false, false, myStatus, hasPendingIncoming, isFollower,user.isPrivateProfile()
        );
    }
}
