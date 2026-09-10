package com.beem.TastyMap.userRelated.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepo extends JpaRepository<BlockEntity,Long> ,BlockRepoCustom {
    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    @Query("SELECT b.id FROM BlockEntity b WHERE b.blocker.id = :myId AND b.blocked.id = :userId")
    Optional<Long> findIdByBlockerIdAndBlockedId(@Param("myId") Long myId, @Param("userId") Long userId);

    @Query("SELECT b.blocker.id FROM BlockEntity b WHERE " +
            "(b.blocker.id = :userA AND b.blocked.id = :userB) OR " +
            "(b.blocker.id = :userB AND b.blocked.id = :userA)")
    List<Long> findBlockerIdsBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BlockEntity b WHERE " +
            "(b.blocker.id = :userId AND b.blocked.id = :targetId) OR " +
            "(b.blocker.id = :targetId AND b.blocked.id = :userId)")
    boolean isBlockExistsBetween(@Param("userId") Long userId, @Param("targetId") Long targetId);
}
