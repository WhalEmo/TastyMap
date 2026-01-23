package com.beem.TastyMap.UserProfile.Subscribe;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscribeService {
    private final SubscribeRepo subscribeRepo;

    public SubscribeService(SubscribeRepo subscribeRepo) {
        this.subscribeRepo = subscribeRepo;
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


}
