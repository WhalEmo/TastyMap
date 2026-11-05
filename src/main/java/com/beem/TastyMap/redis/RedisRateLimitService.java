package com.beem.TastyMap.redis;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class RedisRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);

    private final ProxyManager<String> proxyManager;
    private final RedisCacheService redisCacheService;

    private static final BucketConfiguration AUTH_CONFIG = BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(120, Refill.greedy(120, Duration.ofMinutes(1))))
            .build();


    private static final BucketConfiguration ANON_CONFIG = BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))))
            .build();

    public RedisRateLimitService(
            ProxyManager<String> proxyManager,
            RedisCacheService redisCacheService
    ) {
        this.proxyManager = proxyManager;
        this.redisCacheService = redisCacheService;
    }

    public boolean tryConsume(String key, boolean isAuthenticated) {
        Supplier<BucketConfiguration> configSupplier = () ->
                isAuthenticated ? AUTH_CONFIG : ANON_CONFIG;

        String redisKey = "RATE_LIMIT:" + key;
        try {
            boolean allowed = proxyManager.builder().build(redisKey, configSupplier).tryConsume(1);

            redisCacheService.expire(redisKey, 120);

            return allowed;
        } catch (Exception e) {
            log.error("[RateLimit] Redis erişim hatası, istek geçiriliyor. Key: {}, Hata: {}", redisKey, e.getMessage());
            return true;
        }
    }
}