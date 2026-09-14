package com.beem.TastyMap.user.search.controller;

import com.beem.TastyMap.user.search.service.UserSearchService;
import com.beem.TastyMap.user.search.dto.UserSearchDTO;
import com.beem.TastyMap.user.search.service.SearchHistoryService;
import org.springframework.data.domain.Page;
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
            Authentication authentication
    ) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Long myId = (Long) authentication.getPrincipal();
        List<UserSearchDTO> results = searchService.searchUsers(keyword, myId, page, size);
        return ResponseEntity.ok(results);
    }

    // 2. Boş ekranda son aramaları getir
    @GetMapping("/history")
    public ResponseEntity<Page<UserSearchDTO>> getRecentSearches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        Page<UserSearchDTO> history = searchHistoryService.getRecentSearches(myId, page,size);
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
    @DeleteMapping("/history-delete/{userIdToDelete}")
    public ResponseEntity<Void> deleteFromHistory(
            @PathVariable Long userIdToDelete,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        searchHistoryService.deleteFromHistory(myId, userIdToDelete);
        return ResponseEntity.ok().build();
    }
}