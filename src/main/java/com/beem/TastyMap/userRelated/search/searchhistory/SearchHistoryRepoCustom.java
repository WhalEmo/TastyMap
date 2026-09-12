package com.beem.TastyMap.userRelated.search.searchhistory;

import com.beem.TastyMap.userRelated.search.UserSearchDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchHistoryRepoCustom {
    List<UserSearchDTO> findMyRecentSearches(Long myId, Pageable pageable);
}
