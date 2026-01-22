package com.beem.TastyMap.Maps.Service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;


@Service
public class RedisCacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final long TTL_SECONDS = 3600;

    public RedisCacheService(StringRedisTemplate redis, ObjectMapper objectMapper){
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> T getWithSlidingTTL(String key, Class<T> type, long TTL){
        return redis.execute((RedisCallback<T>) connection ->{
            try {
                byte[] rawKey = redis.getStringSerializer().serialize(key);

                byte[] rawValue = connection.execute(
                        "GETEX",
                        rawKey,
                        "EX".getBytes(),
                        String.valueOf(TTL).getBytes()
                );

                return null;
            } catch (Exception e) {
                return null;
            }
        });
    }

}
