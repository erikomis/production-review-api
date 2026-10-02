package com.client.productionreview.security;

import com.client.productionreview.exception.ErrorResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Proteção contra CSRF para os cookies de sessão: métodos que alteram estado em {@code /api/v1/**}
 * só são aceitos se o {@code Origin} (ou, na falta dele, o {@code Referer}) for uma origem permitida
 * ou a própria API. Sem os dois headers (curl, apps nativos) a requisição passa.
 */
@Slf4j
public class OriginCheckFilter extends OncePerRequestFilter {

    public static final String MESSAGE = "Origem não permitida";

    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final String API_PREFIX = "/api/v1/";

    private final Set<String> allowedOrigins;

    public OriginCheckFilter(Collection<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream()
                .map(OriginCheckFilter::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !UNSAFE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT)) || !path.startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            String referer = request.getHeader(HttpHeaders.REFERER);
            origin = referer == null ? null : originOf(referer);
        }

        if (origin != null && !isAllowed(origin, request)) {
            log.debug("Requisição {} {} recusada: origem {}", request.getMethod(), request.getRequestURI(), origin);
            ErrorResponses.write(response, HttpStatus.FORBIDDEN, MESSAGE);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String origin, HttpServletRequest request) {
        String normalized = normalize(origin);
        // origem permitida ou a própria API (ex.: Swagger UI servido na mesma porta)
        return allowedOrigins.contains(normalized) || normalized.equals(normalize(selfOrigin(request)));
    }

    static String selfOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }

    /** "https://site.com/pagina?x=1" -> "https://site.com"; referer inválido vira "invalid" (recusado). */
    static String originOf(String referer) {
        try {
            URI uri = URI.create(referer.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "invalid";
            }
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException e) {
            return "invalid";
        }
    }

    private static String normalize(String origin) {
        String value = origin.trim().toLowerCase(Locale.ROOT);
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
