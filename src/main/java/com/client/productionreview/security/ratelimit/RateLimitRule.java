package com.client.productionreview.security.ratelimit;

import lombok.Getter;

import java.time.Duration;

/**
 * Regras de limite de requisições (janela fixa). O limite e a janela de cada regra podem ser
 * alterados por propriedade: {@code app.rate-limit.rules.<id>.limit} e {@code .window}.
 */
@Getter
public enum RateLimitRule {
    SIGN_IN_LOGIN("sign-in-login", KeyType.IP_AND_LOGIN, 5, Duration.ofMinutes(1)),
    SIGN_IN_IP("sign-in-ip", KeyType.IP, 20, Duration.ofMinutes(1)),
    SIGN_UP_IP("sign-up-ip", KeyType.IP, 5, Duration.ofHours(1)),
    RECOVERY_SEND_EMAIL("recovery-send-email", KeyType.EMAIL, 3, Duration.ofMinutes(15)),
    RECOVERY_SEND_IP("recovery-send-ip", KeyType.IP, 10, Duration.ofHours(1)),
    RECOVERY_CODE_IP("recovery-code-ip", KeyType.IP, 10, Duration.ofMinutes(15)),
    REVIEW_CREATE_USER("review-create-user", KeyType.USER, 10, Duration.ofHours(1)),
    REVIEW_REPORT_USER("review-report-user", KeyType.USER, 20, Duration.ofHours(1));

    /** De onde vem a identidade contada pela regra. */
    public enum KeyType { IP, IP_AND_LOGIN, EMAIL, USER }

    private final String id;
    private final KeyType keyType;
    private final int defaultLimit;
    private final Duration defaultWindow;

    RateLimitRule(String id, KeyType keyType, int defaultLimit, Duration defaultWindow) {
        this.id = id;
        this.keyType = keyType;
        this.defaultLimit = defaultLimit;
        this.defaultWindow = defaultWindow;
    }
}
