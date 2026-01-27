package com.beem.TastyMap.UserRelated.Block;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/block")
public class BlockController {
    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping("/{userId}")
    public void blockUser(
            @PathVariable Long userId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        blockService.block(userId,myId);
    }

    @DeleteMapping("/unBlock/{userId}")
    public void unBlockUser(
            @PathVariable Long userId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        blockService.unBlock(userId,myId);
    }

    @GetMapping("/getBlocks")
    public Page<BlockDTOResponse> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return blockService.getBlock(myId, page, size);
    }
}
