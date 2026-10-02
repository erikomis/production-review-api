package com.client.productionreview.security.ratelimit;

import java.time.Duration;

/** Contador de janela fixa. */
public interface RateLimitStore {

    /** Soma 1 ao contador da chave (criando-o com a janela informada) e devolve o total e o tempo restante. */
    Hit hit(String key, Duration window);

    record Hit(long count, long ttlMillis) {
    }
}
