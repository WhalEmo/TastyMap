package com.beem.TastyMap.userRelated.post;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import com.beem.TastyMap.userRelated.subscribe.SubscribeRepo;
import org.springframework.stereotype.Component;

@Component
public class AccessChecker {

    private final UserRepo userRepo;
    private final SubscribeRepo subscribeRepo;
    private final BlockRepo blockRepo;

    public AccessChecker(UserRepo userRepo, SubscribeRepo subscribeRepo, BlockRepo blockRepo) {
        this.userRepo = userRepo;
        this.subscribeRepo = subscribeRepo;
        this.blockRepo = blockRepo;
    }

    public void checkAccess(Long targetUserId, Long myId) {
        if (targetUserId.equals(myId)) return;

        Boolean isPrivate = userRepo.isProfilePrivate(targetUserId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        boolean blocked = blockRepo.existsByBlocker_IdAndBlocked_Id(targetUserId, myId) ||
                blockRepo.existsByBlocker_IdAndBlocked_Id(myId, targetUserId);

        if (blocked) {
            throw new CustomExceptions.ForbiddenException("Bu kullanıcının postlarına erişim yok");
        }

        if (isPrivate) {
            boolean isFollowing = subscribeRepo.existsBySubscriber_IdAndSubscribed_Id(myId, targetUserId);
            if (!isFollowing) {
                throw new CustomExceptions.ForbiddenException("Bu kullanıcının postlarını görmek için takip etmelisin");
            }
        }
    }
}
