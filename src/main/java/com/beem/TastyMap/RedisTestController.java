package com.beem.TastyMap;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    private final StringRedisTemplate redisTemplate;

    public RedisTestController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/set")
    public String set() {
        redisTemplate.opsForValue().set("test:key", "Redis OK");
        return "SET OK";
    }

    @GetMapping("/get")
    public String get() {
        return redisTemplate.opsForValue().get("test:key");
    }
}
