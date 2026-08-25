package com.beem.TastyMap.security.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.access-exp-ms:900000}")
    private long accessExpMs;

    private static final String PREFIX = "pwd_changed_at:";

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void invalidateUserSessions(Long userId) {
        String key = PREFIX + userId;
        long currentTimestampSeconds = Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), accessExpMs, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenInvalidated(Long userId, Instant tokenIssuedAt) {
        String key = PREFIX + userId;
        String pwdChangedAtStr = redisTemplate.opsForValue().get(key);

        if (pwdChangedAtStr == null) {
            return false;
        }

        long pwdChangedAtSeconds = Long.parseLong(pwdChangedAtStr);

        long tokenIssuedAtSeconds = tokenIssuedAt.getEpochSecond();

        return tokenIssuedAtSeconds < pwdChangedAtSeconds;
    }
}