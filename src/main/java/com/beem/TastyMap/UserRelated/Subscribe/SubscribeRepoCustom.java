package com.beem.TastyMap.UserRelated.Subscribe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface SubscribeRepoCustom {
    Page<SubscribeDTO> findUserSubscribes(@Param("userId") Long userId, Pageable pageable);
    Page<SubscribeDTO> findUserSubscribers(@Param("userId") Long userId, Pageable pageable
    );
}
