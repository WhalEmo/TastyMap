package com.beem.TastyMap.UserRelated.Block;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BlockRepo extends JpaRepository<BlockEntity,Long> ,BlockRepoCustom {
    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    @Query("SELECT b.id FROM BlockEntity b WHERE b.blocker.id = :myId AND b.blocked.id = :userId")
    Optional<Long> findIdByBlockerIdAndBlockedId(@Param("myId") Long myId, @Param("userId") Long userId);
}
