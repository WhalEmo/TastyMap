package com.beem.TastyMap.userRelated.subscribe;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import com.beem.TastyMap.userRelated.post.AccessChecker;
import jakarta.persistence.EntityManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscribeService {
    private final UserRepo userRepo;
    private final SubscribeRepo subscribeRepo;
    private final AccessChecker accessChecker;
    private final EntityManager entityManager;
    private final BlockRepo blockRepo;
    private final MessageSource messageSource;

    public SubscribeService(UserRepo userRepo, SubscribeRepo subscribeRepo, AccessChecker accessChecker, EntityManager entityManager, BlockRepo blockRepo, MessageSource messageSource) {
        this.userRepo = userRepo;
        this.subscribeRepo = subscribeRepo;
        this.accessChecker = accessChecker;
        this.entityManager = entityManager;
        this.blockRepo = blockRepo;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void subscribe(Long subscribes, Long myId) {
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

        UserEntity subscriberRef = entityManager.getReference(UserEntity.class, myId);
        UserEntity subscribedRef = entityManager.getReference(UserEntity.class, subscribes);

        SubscribeEntity entity = new SubscribeEntity();
        entity.setSubscriber(subscriberRef);
        entity.setSubscribed(subscribedRef);
        entity.setDate(LocalDateTime.now());

        subscribeRepo.save(entity);
        userRepo.updateSubscribedCount(myId, 1);
        userRepo.updateSubscriberCount(subscribes, 1);
    }

    @Transactional
    //abonelikten cıkma metodu
    public void unSubscribe(Long subscribes, Long myId) {
        Long sub = subscribeRepo
                .findIdBySubscriberAndSubscribed(myId, subscribes)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException(getMessage("subscribe.not.found"))
                );
        subscribeRepo.deleteById(sub);
        userRepo.updateSubscribedCount(myId, -1);
        userRepo.updateSubscriberCount(subscribes, -1);
    }

    @Transactional
    //aboneyi cıkarma metodu
    public void unSubscriber(Long subscribes, Long myId) {
        Long sub = subscribeRepo
                .findIdBySubscriberAndSubscribed(subscribes, myId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException(getMessage("subscribe.not.found"))
                );
        subscribeRepo.deleteById(sub);
        userRepo.updateSubscribedCount(subscribes, -1);
        userRepo.updateSubscriberCount(myId, -1);
    }

    //benim abone olduklarım
    public Page<SubscribeDTO> getUserSubscribes(Long userId, Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        accessChecker.checkAccess(userId, myId);
        return subscribeRepo.findUserSubscribes(userId, pageable);
    }

    //bana abone olanlar
    public Page<SubscribeDTO> getUserSubscribers(Long userId, Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        accessChecker.checkAccess(userId, myId);
        return subscribeRepo.findUserSubscribers(userId, pageable);
    }
}