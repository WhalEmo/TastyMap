package com.beem.TastyMap.userRelated.subscribe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface SubscribeRepoCustom {
    Page<SubscribeDTO> findUserSubscribes(Long userId, Long myId, Pageable pageable);
    Page<SubscribeDTO> findUserSubscribers(Long userId, Long myId, Pageable pageable);
    Page<SubscribeDTO> findPendingRequests(Long userId, Pageable pageable);
}
