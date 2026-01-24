package com.beem.TastyMap.UserProfile.Subscribe;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserProfile.Block.BlockDTOResponse;
import com.beem.TastyMap.UserProfile.Block.BlockEntity;
import com.beem.TastyMap.UserProfile.Block.BlockRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscribeService {
    private final SubscribeRepo subscribeRepo;
    private final UserRepo userRepo;
    private final BlockRepo blockRepo;

    public SubscribeService(SubscribeRepo subscribeRepo, UserRepo userRepo, BlockRepo blockRepo) {
        this.subscribeRepo = subscribeRepo;
        this.userRepo = userRepo;
        this.blockRepo = blockRepo;
    }

    //abone olma
    public void subscribe(Long subscribes,Long myId){

        if (subscribeRepo.existsBySubscriberIdAndSubscribedId(myId, subscribes)) {
            throw new CustomExceptions.UserAlreadyExistsException("Zaten abonesin");
        }
        SubscribeEntity entity=new SubscribeEntity();
        entity.setSubscribedId(subscribes);
        entity.setSubscriberId(myId);
        entity.setDate(LocalDateTime.now());
        subscribeRepo.save(entity);
    }
//abonelikten cıkma metodu
    public void unSubscribe(Long subscribes,Long myId){
        SubscribeEntity sub = subscribeRepo
                .findBySubscriberIdAndSubscribedId(myId, subscribes)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Abonelik bulunamadı")
                );
        subscribeRepo.delete(sub);
    }
    //aboneyi cıkarma metodu
    public void unSubscriber(Long subscribes,Long myId){
        SubscribeEntity sub=subscribeRepo
                .findBySubscriberIdAndSubscribedId(subscribes,myId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Abonelik bulunamadı")
                );
        subscribeRepo.delete(sub);
    }
//BAKILACAK DUSUNULECEK
    private void checkProfileAccess(Long profileUserId, Long myId) {
        UserEntity profileUser = userRepo.findById(profileUserId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        if (profileUserId.equals(myId)) return;

        boolean blocked = blockRepo.existsByBlockerIdAndBlockedId(profileUserId, myId) ||
                        blockRepo.existsByBlockerIdAndBlockedId(myId, profileUserId);

        if (blocked) {
            throw new CustomExceptions.ForbiddenException("Bu kullanıcıyla etkileşim yok");
        }

        if (!profileUser.isPrivateProfile()) return;

        boolean isFollowing = subscribeRepo.existsBySubscriberIdAndSubscribedId(myId, profileUserId);

        if (!isFollowing) {
            throw new CustomExceptions.ForbiddenException(
                    "Bu kullanıcının bilgilerini görüntülemek için takip etmelisiniz"
            );
        }
    }

    //benimabone oldukarlım
    public Page<SubscribeDTO> getUserSubscribes(Long userId, Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        checkProfileAccess(userId, myId);
        return subscribeRepo.findUserSubscribes(userId, pageable);
    }

    //bana abone olanlar
    public Page<SubscribeDTO> getUserSubscribers(Long userId, Long myId,int page, int size){
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        checkProfileAccess(userId, myId);
        return subscribeRepo.findUserSubscribers(userId, pageable);
    }
}