package com.client.productionreview.security.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** INCR + EXPIRE no Redis: a primeira requisição da janela define a expiração da chave. */
@Component
public class RedisRateLimitStore implements RateLimitStore {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Hit hit(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        long current = count == null ? 1 : count;
        if (current == 1) {
            redisTemplate.expire(key, window);
        }
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        if (ttl == null || ttl < 0) {
            // chave sem expiração (ex.: falha entre o INCR e o EXPIRE): nunca deixa o bloqueio eterno
            redisTemplate.expire(key, window);
            ttl = window.toMillis();
        }
        return new Hit(current, ttl);
    }
}
