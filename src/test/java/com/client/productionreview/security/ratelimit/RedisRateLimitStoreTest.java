package com.client.productionreview.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RedisRateLimitStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> values;
    private RedisRateLimitStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        store = new RedisRateLimitStore(redisTemplate);
    }

    @Test
    void firstHit_setsTheWindow() {
        when(values.increment("k")).thenReturn(1L);
        when(redisTemplate.getExpire("k", TimeUnit.MILLISECONDS)).thenReturn(60_000L);

        RateLimitStore.Hit hit = store.hit("k", Duration.ofMinutes(1));

        assertEquals(1, hit.count());
        assertEquals(60_000L, hit.ttlMillis());
        verify(redisTemplate).expire("k", Duration.ofMinutes(1));
    }

    @Test
    void nextHits_keepTheWindow() {
        when(values.increment("k")).thenReturn(4L);
        when(redisTemplate.getExpire("k", TimeUnit.MILLISECONDS)).thenReturn(12_000L);

        RateLimitStore.Hit hit = store.hit("k", Duration.ofMinutes(1));

        assertEquals(4, hit.count());
        assertEquals(12_000L, hit.ttlMillis());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void keyWithoutExpiration_getsTheWindowAgain() {
        when(values.increment("k")).thenReturn(3L);
        when(redisTemplate.getExpire("k", TimeUnit.MILLISECONDS)).thenReturn(-1L);

        RateLimitStore.Hit hit = store.hit("k", Duration.ofMinutes(15));

        assertEquals(900_000L, hit.ttlMillis());
        verify(redisTemplate).expire("k", Duration.ofMinutes(15));
    }
}
