package com.beem.TastyMap.user.search.repo;

import com.beem.TastyMap.user.search.dto.UserSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchHistoryRepoCustom {
    Page<UserSearchDTO> findMyRecentSearches(Long myId, Pageable pageable);
}
