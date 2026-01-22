package com.beem.TastyMap.UserProfile.Block;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepo extends JpaRepository<BlockEntity,Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId,Long blockedId);
    Optional<BlockEntity>findByBlockerIdAndBlockedId(Long blockerId,Long blockedId);
    Page<BlockEntity> findByBlockerId(Long blockerId, Pageable pageable);
}
