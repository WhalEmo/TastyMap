package com.beem.TastyMap.UserProfile.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserProfile.Block.BlockRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeRepo;
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

        UserEntity user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        boolean blocked = blockRepo.existsByBlocker_IdAndBlocked_Id(targetUserId, myId) ||
                blockRepo.existsByBlocker_IdAndBlocked_Id(myId, targetUserId);

        if (blocked) {
            throw new CustomExceptions.ForbiddenException("Bu kullanıcının postlarına erişim yok");
        }

        if (user.isPrivateProfile()) {
            boolean isFollowing = subscribeRepo.existsBySubscriber_IdAndSubscribed_Id(myId, targetUserId);
            if (!isFollowing) {
                throw new CustomExceptions.ForbiddenException("Bu kullanıcının postlarını görmek için takip etmelisin");
            }
        }
    }
}
