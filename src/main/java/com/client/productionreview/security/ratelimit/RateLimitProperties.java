package com.client.productionreview.security.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** {@code app.rate-limit.*}: liga/desliga o limite e sobrescreve limite/janela por regra. */
@Data
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Chave = id da regra (ex.: {@code sign-in-login}). */
    private Map<String, Limit> rules = new HashMap<>();

    public int limitOf(RateLimitRule rule) {
        Limit limit = rules.get(rule.getId());
        return limit != null && limit.getLimit() != null ? limit.getLimit() : rule.getDefaultLimit();
    }

    public Duration windowOf(RateLimitRule rule) {
        Limit limit = rules.get(rule.getId());
        return limit != null && limit.getWindow() != null ? limit.getWindow() : rule.getDefaultWindow();
    }

    @Data
    public static class Limit {
        private Integer limit;
        private Duration window;
    }
}
