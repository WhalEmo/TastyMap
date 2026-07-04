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

    public RedisRateLimitService(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
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

        return proxyManager.builder().build(redisKey, configSupplier).tryConsume(1);
    }
}