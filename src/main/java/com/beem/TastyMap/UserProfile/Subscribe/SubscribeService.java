package com.beem.TastyMap.UserProfile.Subscribe;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscribeService {
    private final SubscribeRepo subscribeRepo;
    private final UserRepo userRepo;

    public SubscribeService(SubscribeRepo subscribeRepo, UserRepo userRepo) {
        this.subscribeRepo = subscribeRepo;
        this.userRepo = userRepo;
    }

    //abone olma
    public void subscribe(Long subscribes,Long myId){
        userRepo.findById(subscribes)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

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
}
