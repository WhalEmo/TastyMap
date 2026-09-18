package com.beem.TastyMap.user.search.repo;

import com.beem.TastyMap.user.search.entity.SearchHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepo extends JpaRepository<SearchHistoryEntity, Long>, SearchHistoryRepoCustom {

    Optional<SearchHistoryEntity> findBySearcherIdAndSearchedUserId(Long searcherId, Long searchedUserId);

    void deleteBySearcherIdAndSearchedUserId(Long searcherId, Long searchedUserId);

    @Query("SELECT h.id FROM SearchHistoryEntity h WHERE h.searcher.id = :searcherId ORDER BY h.searchedAt DESC")
    List<Long> findHistoryIdsBySearcherId(@Param("searcherId") Long searcherId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SearchHistoryEntity h WHERE h.id IN :ids")
    void deleteHistoriesByIds(@Param("ids") List<Long> ids);
}