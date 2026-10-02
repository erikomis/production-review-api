package com.client.productionreview.security.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Contador em memória para os testes (mesma semântica do Redis: a primeira batida define a janela). */
public class InMemoryRateLimitStore implements RateLimitStore {

    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    public synchronized Hit hit(String key, Duration window) {
        long now = System.currentTimeMillis();
        long[] counter = counters.get(key);
        if (counter == null || counter[1] <= now) {
            counter = new long[]{0, now + window.toMillis()};
            counters.put(key, counter);
        }
        counter[0]++;
        return new Hit(counter[0], counter[1] - now);
    }

    public void clear() {
        counters.clear();
    }
}
