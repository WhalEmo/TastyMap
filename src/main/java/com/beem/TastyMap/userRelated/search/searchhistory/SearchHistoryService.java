package com.beem.TastyMap.userRelated.search.searchhistory;

import com.beem.TastyMap.registerLogin.UserRepo;
import com.beem.TastyMap.userRelated.search.UserSearchDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchHistoryService {

    private final SearchHistoryRepo historyRepo;
    private final UserRepo userRepo;

    public SearchHistoryService(SearchHistoryRepo historyRepo, UserRepo userRepo) {
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
    }

    // 1. KULLANICIYA TIKLANDIĞINDA ÇALIŞACAK METOT
    @Transactional
    public void addToHistory(Long myId, Long clickedUserId) {
        if (myId.equals(clickedUserId)) return; // Kendini geçmişe ekleme

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

        // İPUCU: Veritabanı şişmesin diye belirli periyotlarda
        // kullanıcının en eski kayıtlarını silebilirsin (Limit: 20)
    }

    // 2. BOŞ ARAMA EKRANINDA GÖSTERİLECEK LİSTE
    public List<UserSearchDTO> getRecentSearches(Long myId) {
        return historyRepo.findMyRecentSearches(myId, PageRequest.of(0, 15));
    }

    @Transactional
    public void deleteFromHistory(Long myId, Long userIdToDelete) {
        historyRepo.deleteBySearcherIdAndSearchedUserId(myId, userIdToDelete);
    }
}
