package com.client.productionreview.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.time.Instant;


@Service
public class JwtProvider {


    private static final String COOKIE = "token";

    private static final String REFRESH_COOKIE = "refresh_token";

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

    public DecodedJWT validateToken(String token) {

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        try {
            var tokenDecoded = JWT.require(algorithm)
                    .build()
                    .verify(token);

            return tokenDecoded;
        } catch (JWTVerificationException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    public ResponseCookie generateToken(Long userId) {

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationRefresh);
        long secondsUntilExpiration = Duration.between(now, expiration).getSeconds();
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        var jwt =JWT.create()
                .withIssuer("Production Review API")
                .withExpiresAt(
                        expiration
                )
                .withSubject(userId.toString())
                .sign(algorithm);

        var builder = ResponseCookie.from(COOKIE, jwt)
                .path("/")
                .maxAge(secondsUntilExpiration)
                .secure(true)
                .httpOnly(true);

        return builder.build();
    }

public ResponseCookie generateRefreshToken(Long userId) {
    Instant now = Instant.now();
    Instant expiration = now.plusSeconds(expirationRefresh);
    long secondsUntilExpiration = Duration.between(now, expiration).getSeconds();


    Algorithm algorithm = Algorithm.HMAC256(secretKey);
        var jwt = JWT.create()
                .withIssuer("Production Review API")
                .withSubject(userId.toString())
                .withExpiresAt(
                        expiration
                )
                .sign(algorithm);

        var builder = ResponseCookie.from(REFRESH_COOKIE, jwt)
            .path("/")
            .maxAge( secondsUntilExpiration)
            .secure(true)
            .httpOnly(true);

        return builder.build();
    }

    public Long getUserId(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return Long.parseLong(decodedJWT.getSubject());
    }

    public Long getUserIdFromRefreshToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return Long.parseLong(decodedJWT.getSubject());
    }

    public boolean isValid(String token) {
        return validateToken(token) != null;
    }

    public boolean isValidRefreshToken(String token) {
        return validateToken(token) != null;
    }


    public ResponseCookie cleanToken() {
        var builder = ResponseCookie.from(COOKIE, "")
                .path("/")
                .maxAge(0)
                .secure(true)
                .httpOnly(true);
        return builder.build();
    }



    public ResponseCookie cleanRefreshToken() {
        var builder = ResponseCookie.from(REFRESH_COOKIE, "")
                .path("/")
                .maxAge(0)
                .secure(true)
                .httpOnly(true);
        return builder.build();
    }
}
