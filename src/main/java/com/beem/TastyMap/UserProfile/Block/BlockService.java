package com.beem.TastyMap.UserProfile.Block;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BlockService {
    private final BlockRepo blockRepo;
    private final UserRepo userRepo;
    private final SubscribeService subscribeService;

    public BlockService(BlockRepo blockRepo, UserRepo userRepo, SubscribeService subscribeService) {
        this.blockRepo = blockRepo;
        this.userRepo = userRepo;
        this.subscribeService = subscribeService;
    }
    @Transactional
    public void block(Long userId,Long myId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        if (userId.equals(myId)) {
            throw new CustomExceptions.InvalidException("Kendini engelleyemezsin");
        }
        if(blockRepo.existsByBlockerIdAndBlockedId(myId,userId)){
            throw new CustomExceptions.UserAlreadyExistsException("Zaten engellenmiş");
        }
        BlockEntity block=new BlockEntity();
        block.setBlockedId(userId);
        block.setBlockerId(myId);

        blockRepo.save(block);

        subscribeService.unSubscribe(userId, myId);
        subscribeService.unSubscriber(userId, myId);
    }

    public void unBlock(Long userId,Long myId){
        BlockEntity block=blockRepo
                .findByBlockerIdAndBlockedId(myId,userId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Engel bulunamadı")
                );
        blockRepo.delete(block);
    }

    public Page<BlockDTOResponse> getBlock(Long myId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blockRepo.findMyBlocks(myId, pageable);
    }

}
