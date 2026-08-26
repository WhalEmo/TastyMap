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

    private static final String USER_PREFIX = "user_all_sessions_invalidated_at:";
    private static final String DEVICE_PREFIX = "device_session_invalidated_at:";
    private static final String PASSWORD_CHANGED_PREFIX = "device_password_changed_at:";

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public enum InvalidationReason {
        NONE,
        PASSWORD_CHANGED,
        LOGGED_OUT
    }

    public void invalidateAllUserSessions(Long userId) {
        String key = USER_PREFIX + userId;
        long currentTimestampSeconds = Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), accessExpMs, TimeUnit.MILLISECONDS);
    }

    public void invalidateDeviceSession(Long userId, String deviceId) {
        String key = DEVICE_PREFIX + userId + ":" + deviceId;
        long currentTimestampSeconds = Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), accessExpMs, TimeUnit.MILLISECONDS);
    }

    public void invalidateDevicePasswordChanged(Long userId, String deviceId) {
        String key = PASSWORD_CHANGED_PREFIX + userId + ":" + deviceId;
        long currentTimestampSeconds = Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), accessExpMs, TimeUnit.MILLISECONDS);
    }

    public InvalidationReason getInvalidationReason(Long userId, String deviceId, Instant tokenIssuedAt) {
        long tokenIssuedAtSeconds = tokenIssuedAt.getEpochSecond();

        String userInvalidatedAtStr = redisTemplate.opsForValue().get(USER_PREFIX + userId);
        if (userInvalidatedAtStr != null) {
            long userInvalidatedAt = Long.parseLong(userInvalidatedAtStr);
            if (tokenIssuedAtSeconds < userInvalidatedAt) {
                return InvalidationReason.PASSWORD_CHANGED;
            }
        }

        if (deviceId != null) {
            String pwdInvalidatedAtStr = redisTemplate.opsForValue().get(PASSWORD_CHANGED_PREFIX + userId + ":" + deviceId);
            if (pwdInvalidatedAtStr != null) {
                long pwdInvalidatedAt = Long.parseLong(pwdInvalidatedAtStr);
                if (tokenIssuedAtSeconds < pwdInvalidatedAt) {
                    return InvalidationReason.PASSWORD_CHANGED;
                }
            }

            String deviceInvalidatedAtStr = redisTemplate.opsForValue().get(DEVICE_PREFIX + userId + ":" + deviceId);
            if (deviceInvalidatedAtStr != null) {
                long deviceInvalidatedAt = Long.parseLong(deviceInvalidatedAtStr);
                if (tokenIssuedAtSeconds < deviceInvalidatedAt) {
                    return InvalidationReason.LOGGED_OUT;
                }
            }
        }

        return InvalidationReason.NONE;
    }
}