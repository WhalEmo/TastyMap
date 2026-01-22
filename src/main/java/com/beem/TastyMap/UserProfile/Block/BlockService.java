package com.beem.TastyMap.UserProfile.Block;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockService {
    private final BlockRepo blockRepo;
    private final UserRepo userRepo;

    public BlockService(BlockRepo blockRepo, UserRepo userRepo) {
        this.blockRepo = blockRepo;
        this.userRepo = userRepo;
    }

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
    }
    public void unBlock(Long userId,Long myId){
        BlockEntity block=blockRepo
                .findByBlockerIdAndBlockedId(myId,userId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Engel bulunamadı")
                );
        blockRepo.delete(block);
    }

    public Page<BlockDTOResponse> getBlock(Long myId,int page, int size){
        Pageable pageable= PageRequest.of(page,size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BlockEntity> blocks = blockRepo.findByBlockerId(myId,pageable);

        return blocks.map(block->{
            UserEntity user=userRepo.findById(block.getBlockedId())
                    .orElseThrow();
            BlockDTOResponse dto=new BlockDTOResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getProfile(),
                    block.getCreatedAt()
            );
            return dto;
        });
    }
}
