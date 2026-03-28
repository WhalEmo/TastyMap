package com.beem.TastyMap.UserRelated.Block;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserRelated.Subscribe.SubscribeRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockService {
    private final BlockRepo blockRepo;
    private final UserRepo userRepo;
    private final EntityManager entityManager;
    private final SubscribeRepo subscribeRepo;

    public BlockService(BlockRepo blockRepo, UserRepo userRepo, EntityManager entityManager, SubscribeRepo subscribeRepo) {
        this.blockRepo = blockRepo;
        this.userRepo = userRepo;
        this.entityManager = entityManager;
        this.subscribeRepo = subscribeRepo;
    }

    @Transactional
    public void block(Long userId, Long myId) {
        if (userId.equals(myId)) {
            throw new CustomExceptions.InvalidException("Kendini engelleyemezsin");
        }
        try {
            UserEntity blockerRef = entityManager.getReference(UserEntity.class, myId);
            UserEntity blockedRef = entityManager.getReference(UserEntity.class, userId);

            BlockEntity block = new BlockEntity();
            block.setBlocked(blockedRef);
            block.setBlocker(blockerRef);

            blockRepo.saveAndFlush(block);

            handleUnsubscribe(myId, userId);
            handleUnsubscribe(userId, myId);

        } catch (DataIntegrityViolationException e) {
            throw new CustomExceptions.UserAlreadyExistsException("Zaten engellenmiş veya kullanıcı bulunamadı");
        } catch (EntityNotFoundException e) {
            throw new CustomExceptions.NotFoundException("Kullanıcı bulunamadı");
        }
    }

    private void handleUnsubscribe(Long subscriber, Long subscribed) {
        if (subscribeRepo.deleteAndCount(subscriber, subscribed) > 0) {
            userRepo.updateSubscribedCount(subscriber, -1);
            userRepo.updateSubscriberCount(subscribed, -1);
        }
    }

    public void unBlock(Long userId,Long myId){
        Long block=blockRepo
                .findIdByBlockerIdAndBlockedId(myId,userId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Engel bulunamadı")
                );
        blockRepo.deleteById(block);
    }

    public Page<BlockDTOResponse> getBlock(Long myId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blockRepo.findMyBlocks(myId, pageable);
    }

}
