package com.beem.TastyMap.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;


@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;


    public RedisCacheService(StringRedisTemplate redis, ObjectMapper objectMapper){
        this.redis = redis;
        this.objectMapper = objectMapper;
    }


    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Redis GET hatası (key: {}): {}", key, e.getMessage());
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Redis GET (TypeReference) hatası (key: {}): {}", key, e.getMessage());
            return null;
        }
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

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key));
        } catch (Exception e) {
            return false;
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

    public Boolean expire(String key, long TTL) {
        try {
            return redis.expire(key, Duration.ofSeconds(TTL));
        } catch (Exception e) {
            return false;
        }
    }

    public <T> Boolean setIfAbsent(String key, T value, long TTL){
        try {
            String json = objectMapper.writeValueAsString(value);
            return redis.opsForValue().setIfAbsent(key, json, Duration.ofSeconds(TTL));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void delete(String key) {
        redis.delete(key);
    }


    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("Redis pattern ile eşleşen {} anahtar silindi: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Redis deleteByPattern hatası (pattern: {}): {}", pattern, e.getMessage());
        }
    }

}
