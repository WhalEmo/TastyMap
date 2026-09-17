package com.beem.TastyMap.user.search.service;

import com.beem.TastyMap.user.account.repo.UserRepo;
import com.beem.TastyMap.user.search.dto.UserSearchDTO;
import com.beem.TastyMap.user.search.entity.SearchHistoryEntity;
import com.beem.TastyMap.user.search.repo.SearchHistoryRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchHistoryService {

    private final SearchHistoryRepo historyRepo;
    private final UserRepo userRepo;
    private static final int MAX_HISTORY_LIMIT = 20;

    public SearchHistoryService(SearchHistoryRepo historyRepo, UserRepo userRepo) {
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public void addToHistory(Long myId, Long clickedUserId) {
        //if (myId.equals(clickedUserId)) return; // Kendini geçmişe ekleme

        SearchHistoryEntity history = historyRepo
                .findBySearcherIdAndSearchedUserId(myId, clickedUserId)
                .orElseGet(() -> {
                    SearchHistoryEntity newHistory = new SearchHistoryEntity();
                    newHistory.setSearcher(userRepo.getReferenceById(myId));
                    newHistory.setSearchedUser(userRepo.getReferenceById(clickedUserId));
                    return newHistory;
                });

        history.setSearchedAt(LocalDateTime.now());
        historyRepo.save(history);

        // Limit kontrolünü çağır
        maintainHistoryLimit(myId);
    }

    private void maintainHistoryLimit(Long myId) {
        Pageable offsetPage = PageRequest.of(1, MAX_HISTORY_LIMIT);

        List<Long> oldHistoryIds = historyRepo.findHistoryIdsBySearcherId(myId, offsetPage);

        if (oldHistoryIds != null && !oldHistoryIds.isEmpty()) {
            historyRepo.deleteHistoriesByIds(oldHistoryIds);
        }
    }

    public Page<UserSearchDTO> getRecentSearches(Long myId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "searchedAt"));
        return historyRepo.findMyRecentSearches(myId, pageable);
    }

    @Transactional
    public void deleteFromHistory(Long myId, Long userIdToDelete) {
        historyRepo.deleteBySearcherIdAndSearchedUserId(myId, userIdToDelete);
    }
}
