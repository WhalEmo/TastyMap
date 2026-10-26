package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.SearchSession;
import com.beem.TastyMap.redis.RedisCacheService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchSessionService {

    private final RedisCacheService redisCacheService;
    private static final String SESSION_PREFIX = "rag:session:";
    private static final long SESSION_TTL_SECONDS = 15 * 60; // 15 dakika (900 saniye)

    public SearchSessionService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public void saveSession(Long userId, String query, List<String> placeIds) {
        String key = SESSION_PREFIX + userId;
        SearchSession session = new SearchSession(query, placeIds, 0);
        redisCacheService.set(key, session, SESSION_TTL_SECONDS);
    }

    public SearchSession getSession(Long userId) {
        String key = SESSION_PREFIX + userId;
        return redisCacheService.get(key, SearchSession.class);
    }

    public void updateIndex(Long userId, SearchSession session, int newIndex) {
        String key = SESSION_PREFIX + userId;

        SearchSession updatedSession = session.withCurrentIndex(newIndex);
        redisCacheService.set(key, updatedSession, SESSION_TTL_SECONDS);
    }

    public void clearSession(Long userId) {
        redisCacheService.delete(SESSION_PREFIX + userId);
    }
}