package com.client.productionreview.security.ratelimit;

import com.client.productionreview.exception.ErrorResponses;
import com.client.productionreview.metrics.BusinessMetrics;
import com.client.productionreview.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static com.client.productionreview.security.ratelimit.RateLimitRule.*;

/**
 * Limite de requisições por IP, login, e-mail ou usuário (janela fixa no Redis).
 * Ao estourar: 429 no formato de erro da API + {@code Retry-After}. Falha no Redis não bloqueia ninguém.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final String KEY_PREFIX = "rate-limit:";

    /** Corpos de login/recuperação são pequenos; o resto é descartado. */
    static final int MAX_BODY_BYTES = 16 * 1024;

    private static final String API = "/api/v1";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private record Route(String method, String pattern, List<RateLimitRule> rules, String bodyField) {
    }

    private static final List<Route> ROUTES = List.of(
            new Route("POST", API + "/auth/sign-in", List.of(SIGN_IN_LOGIN, SIGN_IN_IP), "username"),
            new Route("POST", API + "/auth/sign-up", List.of(SIGN_UP_IP), null),
            new Route("POST", API + "/auth/send-recovery-code/send", List.of(RECOVERY_SEND_EMAIL, RECOVERY_SEND_IP), "email"),
            new Route("GET", API + "/auth/recovery-code", List.of(RECOVERY_CODE_IP), null),
            new Route("PATCH", API + "/auth/recovery-code/password", List.of(RECOVERY_CODE_IP), null),
            new Route("POST", API + "/review/", List.of(REVIEW_CREATE_USER), null),
            new Route("POST", API + "/review/{id}/report", List.of(REVIEW_REPORT_USER), null));

    private final RateLimitStore store;

    private final RateLimitProperties properties;

    private final BusinessMetrics metrics;

    public RateLimitFilter(RateLimitStore store, RateLimitProperties properties, BusinessMetrics metrics) {
        this.store = store;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Route route = properties.isEnabled() ? match(request) : null;
        if (route == null) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest current = request;
        String bodyValue = null;
        if (route.bodyField() != null) {
            CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request, MAX_BODY_BYTES);
            bodyValue = readField(cached.getBody(), route.bodyField());
            current = cached;
        }

        long retryAfter = 0;
        RateLimitRule exceeded = null;
        for (RateLimitRule rule : route.rules()) {
            String identity = identity(rule, request, bodyValue);
            if (identity == null) {
                continue;
            }
            Duration window = properties.windowOf(rule);
            RateLimitStore.Hit hit;
            try {
                hit = store.hit(KEY_PREFIX + rule.getId() + ":" + identity, window);
            } catch (Exception e) {
                log.warn("Rate limit indisponível ({}): {}", rule.getId(), e.getMessage());
                continue;
            }
            if (hit.count() > properties.limitOf(rule)) {
                long seconds = Math.max(1, (hit.ttlMillis() + 999) / 1000);
                if (seconds > retryAfter) {
                    retryAfter = seconds;
                    exceeded = rule;
                }
            }
        }

        if (exceeded != null) {
            metrics.rateLimitRejected(exceeded.getId());
            log.info("Rate limit {} excedido por {} em {} {}", exceeded.getId(), request.getRemoteAddr(),
                    request.getMethod(), request.getRequestURI());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            ErrorResponses.write(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente novamente em " + retryAfter + " segundos.");
            return;
        }
        filterChain.doFilter(current, response);
    }

    private static Route match(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        return ROUTES.stream()
                .filter(route -> route.method().equals(method) && MATCHER.match(route.pattern(), path))
                .findFirst()
                .orElse(null);
    }

    /** Null quando a regra não se aplica (ex.: regra por usuário sem login: a rota responde 401). */
    private static String identity(RateLimitRule rule, HttpServletRequest request, String bodyValue) {
        String ip = request.getRemoteAddr();
        return switch (rule.getKeyType()) {
            case IP -> ip;
            case IP_AND_LOGIN -> ip + ":" + (bodyValue == null ? "-" : bodyValue);
            case EMAIL -> bodyValue;
            case USER -> {
                Long userId = CurrentUser.id();
                yield userId == null ? null : String.valueOf(userId);
            }
        };
    }

    private static String readField(byte[] body, String field) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            JsonNode value = node == null ? null : node.get(field);
            if (value == null || !value.isTextual() || value.asText().isBlank()) {
                return null;
            }
            String normalized = value.asText().trim().toLowerCase(Locale.ROOT);
            return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
        } catch (IOException e) {
            return null;
        }
    }
}
