package com.client.productionreview.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Instant;


@Slf4j
@Service
public class JwtProvider {

    private static final String ISSUER = "Production Review API";

    private static final String COOKIE = "token";

    private static final String REFRESH_COOKIE = "refresh_token";

    private static final String TYPE_CLAIM = "type";

    private static final String ACCESS_TYPE = "access";

    private static final String REFRESH_TYPE = "refresh";

    @Value("${security.token.secret}")
    private String secretKey;

    @Value("${security.token.expiration}")
    private Long expiration;

    @Value("${security.token.expiration.refresh}")
    private Long expirationRefresh;

    public String getTokenFromCookie(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, COOKIE);
        return cookie != null ? cookie.getValue() : null;
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, REFRESH_COOKIE);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * Valida assinatura, emissor, expiração e o tipo do token (access/refresh).
     * Retorna null quando o token é inválido.
     */
    private DecodedJWT validateToken(String token, String type) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return JWT.require(algorithm())
                    .withIssuer(ISSUER)
                    .withClaim(TYPE_CLAIM, type)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException ex) {
            log.debug("Token inválido: {}", ex.getMessage());
            return null;
        }
    }

    public ResponseCookie generateToken(Long userId) {
        return buildCookie(COOKIE, createToken(userId, ACCESS_TYPE, expiration), expiration);
    }

    public ResponseCookie generateRefreshToken(Long userId) {
        return buildCookie(REFRESH_COOKIE, createToken(userId, REFRESH_TYPE, expirationRefresh), expirationRefresh);
    }

    public Long getUserId(String token) {
        return subjectAsLong(validateToken(token, ACCESS_TYPE));
    }

    public Long getUserIdFromRefreshToken(String token) {
        return subjectAsLong(validateToken(token, REFRESH_TYPE));
    }

    public boolean isValid(String token) {
        return validateToken(token, ACCESS_TYPE) != null;
    }

    public boolean isValidRefreshToken(String token) {
        return validateToken(token, REFRESH_TYPE) != null;
    }

    public ResponseCookie cleanToken() {
        return buildCookie(COOKIE, "", 0L);
    }

    public ResponseCookie cleanRefreshToken() {
        return buildCookie(REFRESH_COOKIE, "", 0L);
    }

    private String createToken(Long userId, String type, Long seconds) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withClaim(TYPE_CLAIM, type)
                .withExpiresAt(Instant.now().plusSeconds(seconds))
                .sign(algorithm());
    }

    private ResponseCookie buildCookie(String name, String value, Long maxAge) {
        return ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .secure(true)
                .httpOnly(true)
                .build();
    }

    private Long subjectAsLong(DecodedJWT decodedJWT) {
        if (decodedJWT == null) {
            return null;
        }
        try {
            return Long.parseLong(decodedJWT.getSubject());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(secretKey);
    }
}
