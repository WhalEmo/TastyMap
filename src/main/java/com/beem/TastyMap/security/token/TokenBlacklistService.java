package com.beem.TastyMap.security.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    @Value("${JWT_EXP_MS_ACCESS:900000}")
    private long accessExpMs;

    private static final String USER_PREFIX = "user_all_sessions_invalidated_at:";
    private static final String DEVICE_PREFIX = "device_session_invalidated_at:";
    private static final String PASSWORD_CHANGED_PREFIX = "device_password_changed_at:";
    private static final String PWD_EXCEPT_DEVICE_PREFIX = "user_pwd_changed_except:";

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
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), Duration.ofMillis(accessExpMs));
    }

    // ŞİFRE DEĞİŞTİĞİNDE: Şifreyi değiştiren cihaz dışındaki HERKESİ iptal eder
    public void invalidateUserSessionsExceptCurrentDevice(Long userId, String currentDeviceId) {
        if (currentDeviceId == null) {
            invalidateAllUserSessions(userId);
            return;
        }
        String key = PWD_EXCEPT_DEVICE_PREFIX + userId + ":" + currentDeviceId;
        long currentTimestampSeconds = Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), Duration.ofMillis(accessExpMs));
    }

    public void invalidateDeviceSession(Long userId, String deviceId) {
        String key = DEVICE_PREFIX + userId + ":" + deviceId;
        long currentTimestampSeconds = Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, String.valueOf(currentTimestampSeconds), Duration.ofMillis(accessExpMs));
    }

    public InvalidationReason getInvalidationReason(Long userId, String deviceId, Instant tokenIssuedAt) {
        long tokenIssuedAtSeconds = tokenIssuedAt.getEpochSecond();

        // 1. Tüm oturumlar iptal edildi mi?
        String userInvalidatedAtStr = redisTemplate.opsForValue().get(USER_PREFIX + userId);
        if (userInvalidatedAtStr != null) {
            long userInvalidatedAt = Long.parseLong(userInvalidatedAtStr);
            if (tokenIssuedAtSeconds < userInvalidatedAt) {
                return InvalidationReason.PASSWORD_CHANGED;
            }
        }

        // 2. Şifre değişti ama BU CİHAZ HARİÇ mi?
        Set<String> pwdExceptKeys = redisTemplate.keys(PWD_EXCEPT_DEVICE_PREFIX + userId + ":*");
        if (pwdExceptKeys != null) {
            for (String key : pwdExceptKeys) {
                String safeDeviceId = key.substring(key.lastIndexOf(":") + 1);
                // İstek atan cihaz şifreyi değiştiren cihaz DEĞİLSE
                if (!safeDeviceId.equals(deviceId)) {
                    String pwdInvalidatedAtStr = redisTemplate.opsForValue().get(key);
                    if (pwdInvalidatedAtStr != null) {
                        long pwdInvalidatedAt = Long.parseLong(pwdInvalidatedAtStr);
                        if (tokenIssuedAtSeconds < pwdInvalidatedAt) {
                            return InvalidationReason.PASSWORD_CHANGED;
                        }
                    }
                }
            }
        }

        if (deviceId != null) {
            // 3. Tekil cihaz oturum kapatma kontrolü
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