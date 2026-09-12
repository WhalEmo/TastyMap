package com.beem.TastyMap.userRelated.search;

import com.beem.TastyMap.userRelated.search.searchhistory.SearchHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class UserSearchController {

    private final UserSearchService searchService;
    private final SearchHistoryService searchHistoryService;

    public UserSearchController(UserSearchService searchService, SearchHistoryService searchHistoryService) {
        this.searchService = searchService;
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSearchDTO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication // JWT veya Session'dan giren kullanıcının bilgisi
    ) {
        // 1. Boş arama kontrolü (Backend'i yormamak için)
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Long myId = (Long) authentication.getPrincipal();
        List<UserSearchDTO> results = searchService.searchUsers(keyword, myId, page, size);
        return ResponseEntity.ok(results);
    }

    // 2. Boş ekranda son aramaları getir
    @GetMapping("/history")
    public ResponseEntity<List<UserSearchDTO>> getRecentSearches(Authentication authentication) {
        Long myId = (Long) authentication.getPrincipal();
        List<UserSearchDTO> history = searchHistoryService.getRecentSearches(myId);
        return ResponseEntity.ok(history);
    }

    // 3. Bir kullanıcıya tıklandığında geçmişe ekle
    @PostMapping("/history/{clickedUserId}")
    public ResponseEntity<Void> addToHistory(
            @PathVariable Long clickedUserId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        searchHistoryService.addToHistory(myId, clickedUserId);
        return ResponseEntity.ok().build();
    }

    // 4. Geçmişten tek bir kullanıcıyı sil (X butonuna basınca)
    @DeleteMapping("/history/{userIdToDelete}")
    public ResponseEntity<Void> deleteFromHistory(
            @PathVariable Long userIdToDelete,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        searchHistoryService.deleteFromHistory(myId, userIdToDelete);
        return ResponseEntity.ok().build();
    }
}