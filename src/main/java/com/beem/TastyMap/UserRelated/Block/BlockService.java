package com.beem.TastyMap.UserRelated.Block;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserRelated.Subscribe.SubscribeRepo;
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
    private final SubscribeRepo subscribeRepo;

    public BlockService(BlockRepo blockRepo, UserRepo userRepo, SubscribeRepo subscribeRepo) {
        this.blockRepo = blockRepo;
        this.userRepo = userRepo;
        this.subscribeRepo = subscribeRepo;
    }


    @Transactional
    public void block(Long userId,Long myId) {
        UserEntity blocker = userRepo.findById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        UserEntity blocked = userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));


        if (userId.equals(myId)) {
            throw new CustomExceptions.InvalidException("Kendini engelleyemezsin");
        }
        if(blockRepo.existsByBlocker_IdAndBlocked_Id(myId,userId)){
            throw new CustomExceptions.UserAlreadyExistsException("Zaten engellenmiş");
        }
        BlockEntity block=new BlockEntity();
        block.setBlocked(blocked);
        block.setBlocker(blocker);

        blockRepo.save(block);
        subscribeRepo.deleteMutualSubscribe(myId, userId);
    }

    public void unBlock(Long userId,Long myId){
        BlockEntity block=blockRepo
                .findByBlocker_IdAndBlocked_Id(myId,userId)
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
