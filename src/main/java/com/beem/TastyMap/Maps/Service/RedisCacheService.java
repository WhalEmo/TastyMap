package com.beem.TastyMap.Maps.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;


@Service
public class RedisCacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;


    public RedisCacheService(StringRedisTemplate redis, ObjectMapper objectMapper){
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> T getWithSlidingTTL(
            String key,
            TypeReference<T> type,
            long TTL
    ){
        try {
            String json = redis
                    .opsForValue()
                    .getAndExpire(key, Duration.ofSeconds(TTL));
            if(json == null) return null;

            return objectMapper.readValue(json, type);
        }catch (Exception exception){
            throw new RuntimeException(exception);
        }
    }

    public <T> void set(String key, T value, long TTL){
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, Duration.ofSeconds(TTL));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
