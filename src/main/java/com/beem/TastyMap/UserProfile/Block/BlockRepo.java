package com.beem.TastyMap.UserProfile.Block;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepo extends JpaRepository<BlockEntity,Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId,Long blockedId);
    Optional<BlockEntity>findByBlockerIdAndBlockedId(Long blockerId,Long blockedId);
    @Query("""
    SELECT new com.beem.TastyMap.UserProfile.Block.BlockDTOResponse(
        u.id,
        u.username,
        u.profile,
        b.createdAt
    )
    FROM BlockEntity b
    JOIN UserEntity u ON u.id = b.blockedId
    WHERE b.blockerId = :myId
    """)
    Page<BlockDTOResponse> findMyBlocks(
            @Param("myId") Long myId,
            Pageable pageable
    );

}
