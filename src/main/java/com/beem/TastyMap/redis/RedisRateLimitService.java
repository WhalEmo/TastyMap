package com.beem.TastyMap.redis;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class RedisRateLimitService {

    private final ProxyManager<String> proxyManager;
    private final RedisCacheService redisCacheService;

    public RedisRateLimitService(
            ProxyManager<String> proxyManager,
            RedisCacheService redisCacheService
    ) {
        this.proxyManager = proxyManager;
        this.redisCacheService = redisCacheService;
    }

    public boolean tryConsume(String key, boolean isAuthenticated) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            if (isAuthenticated) {
                return BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                        .build();
            } else {
                return BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
                        .build();
            }
        };
        String redisKey = "RATE_LIMIT:" + key;
        boolean allowed = proxyManager.builder().build(redisKey, configSupplier).tryConsume(1);

        redisCacheService.expire(redisKey, 120);

        return allowed;
    }
}