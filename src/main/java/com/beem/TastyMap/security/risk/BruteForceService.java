package com.beem.TastyMap.security.risk;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;

@Service
public class BruteForceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MINUTES = 30;

    public BruteForceService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void registerFailedAttempt(String username) {
        String key = "login_attempts:" + username;
        Object value = redisTemplate.opsForValue().get(key);
        Integer attempts = (value instanceof Integer) ? (Integer) value : null;
        int newAttempts = (attempts == null) ? 1 : attempts + 1;

        redisTemplate.opsForValue().set(key, newAttempts, Duration.ofMinutes(BLOCK_TIME_MINUTES));
    }

    public boolean isBlocked(String username) {
        Integer attempts = (Integer) redisTemplate.opsForValue().get("login_attempts:" + username);
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }

    public void resetAttempts(String username) {
        redisTemplate.delete("login_attempts:" + username);
    }
}