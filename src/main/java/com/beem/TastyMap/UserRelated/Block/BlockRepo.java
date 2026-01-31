package com.beem.TastyMap.UserRelated.Block;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BlockRepo extends JpaRepository<BlockEntity,Long> {
    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    Optional<BlockEntity> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    @Query("""
    SELECT new com.beem.TastyMap.UserRelated.Block.BlockDTOResponse(
        u.id,
        u.username,
        u.profile,
        b.createdAt
    )
    FROM BlockEntity b
    JOIN b.blocked u
    WHERE b.blocker.id = :myId
""")
    Page<BlockDTOResponse> findMyBlocks(
            @Param("myId") Long myId,
            Pageable pageable
    );

}
