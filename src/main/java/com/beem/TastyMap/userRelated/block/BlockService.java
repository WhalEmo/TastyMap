package com.beem.TastyMap.userRelated.block;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.userRelated.socialnotifications.SocialNotificationService;
import com.beem.TastyMap.userRelated.subscribe.SubscribeRepo;
import com.beem.TastyMap.userRelated.subscribe.SubscribeStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockService {
    private final BlockRepo blockRepo;
    private final UserRepo userRepo;
    private final EntityManager entityManager;
    private final SubscribeRepo subscribeRepo;
    private final MessageSource messageSource;
    private final SocialNotificationService socialNotificationService;

    public BlockService(BlockRepo blockRepo,
                        UserRepo userRepo,
                        EntityManager entityManager,
                        SubscribeRepo subscribeRepo,
                        MessageSource messageSource, SocialNotificationService socialNotificationService) {
        this.blockRepo = blockRepo;
        this.userRepo = userRepo;
        this.entityManager = entityManager;
        this.subscribeRepo = subscribeRepo;
        this.messageSource = messageSource;
        this.socialNotificationService = socialNotificationService;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void block(Long userId, Long myId) {
        if (userId.equals(myId)) {
            throw new CustomExceptions.InvalidException(getMessage("block.cannot.block.self"));
        }
        try {
            UserEntity blockerRef = entityManager.getReference(UserEntity.class, myId);
            UserEntity blockedRef = entityManager.getReference(UserEntity.class, userId);

            BlockEntity block = new BlockEntity();
            block.setBlocked(blockedRef);
            block.setBlocker(blockerRef);

            blockRepo.saveAndFlush(block);

            safeUnsubscribe(myId, userId);
            safeUnsubscribe(userId, myId);

            socialNotificationService.clearFollowNotificationsBetween(myId, userId);
        } catch (DataIntegrityViolationException e) {
            throw new CustomExceptions.UserAlreadyExistsException(getMessage("block.already.exists.or.user.not.found"));
        } catch (EntityNotFoundException e) {
            throw new CustomExceptions.NotFoundException(getMessage("user.not.found.simple"));
        }
    }

    private void safeUnsubscribe(Long subscriberId, Long subscribedId) {
        subscribeRepo.findBySubscriber_IdAndSubscribed_Id(subscriberId, subscribedId)
                .ifPresent(entity -> {
                    boolean wasAccepted = entity.getStatus() == SubscribeStatus.ACCEPTED;

                    subscribeRepo.delete(entity);

                    if (wasAccepted) {
                        userRepo.updateSubscribedCount(subscriberId, -1);
                        userRepo.updateSubscriberCount(subscribedId, -1);
                    }
                });
    }

    public void unBlock(Long userId, Long myId) {
        Long block = blockRepo
                .findIdByBlockerIdAndBlockedId(myId, userId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException(getMessage("block.not.found"))
                );
        blockRepo.deleteById(block);
    }

    public Page<BlockDTOResponse> getBlock(Long myId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blockRepo.findMyBlocks(myId, pageable);
    }
}