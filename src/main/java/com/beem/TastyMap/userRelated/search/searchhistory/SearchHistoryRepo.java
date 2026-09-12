package com.beem.TastyMap.userRelated.search.searchhistory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SearchHistoryRepo extends JpaRepository<SearchHistoryEntity, Long>, SearchHistoryRepoCustom {

    Optional<SearchHistoryEntity> findBySearcherIdAndSearchedUserId(Long searcherId, Long searchedUserId);

    void deleteBySearcherIdAndSearchedUserId(Long searcherId, Long searchedUserId);

    void deleteAllBySearcherId(Long searcherId);
}