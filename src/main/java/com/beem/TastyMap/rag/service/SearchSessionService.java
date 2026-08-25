package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.SearchSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_PREFIX = "rag:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(15); //Oturum15 dk geçerli

    public void saveSession(Long userId, String query, List<String> placeIds) {
        String key = SESSION_PREFIX + userId;
        SearchSession session = new SearchSession(query, placeIds, 0);
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);
    }

    public SearchSession getSession(Long userId) {
        String key = SESSION_PREFIX + userId;
        return (SearchSession) redisTemplate.opsForValue().get(key);
    }

    public void updateIndex(Long userId, SearchSession session, int newIndex) {
        String key = SESSION_PREFIX + userId;
        session.setCurrentIndex(newIndex);
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);
    }

    public void clearSession(Long userId) {
        redisTemplate.delete(SESSION_PREFIX + userId);
    }
}