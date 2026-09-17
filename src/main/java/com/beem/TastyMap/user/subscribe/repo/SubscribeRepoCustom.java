package com.beem.TastyMap.user.subscribe.repo;

import com.beem.TastyMap.user.subscribe.dto.SubscribeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscribeRepoCustom {
    Page<SubscribeDTO> findUserSubscribes(Long userId, Long myId, Pageable pageable);
    Page<SubscribeDTO> findUserSubscribers(Long userId, Long myId, Pageable pageable);
    Page<SubscribeDTO> findPendingRequests(Long userId, Pageable pageable);
}
