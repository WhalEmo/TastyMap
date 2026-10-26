package com.beem.TastyMap.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
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

    public <T> void appendToListWithLimit(String key, T item, int maxSize, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(item);
            ListOperations<String, String> listOps = redis.opsForList();

            listOps.rightPush(key, json);

            listOps.trim(key, -maxSize, -1);

            // Oturumu canlı tutmak için her mesajda TTL'i yenile
            redis.expire(key, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error("Redis appendToListWithLimit hatası (key: {}): {}", key, e.getMessage());
        }
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        try {
            List<String> rawList = redis.opsForList().range(key, 0, -1);
            if (rawList == null || rawList.isEmpty()) {
                return Collections.emptyList();
            }
            return rawList.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, clazz);
                        } catch (Exception e) {
                            log.error("Redis JSON parse hatası: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Redis getList hatası (key: {}): {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

}
