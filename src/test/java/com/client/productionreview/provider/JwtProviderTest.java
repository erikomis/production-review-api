package com.client.productionreview.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secretKey", "test-secret");
        ReflectionTestUtils.setField(jwtProvider, "expiration", 900L);
        ReflectionTestUtils.setField(jwtProvider, "expirationRefresh", 57600L);
    }

    @Test
    void accessToken_isValidAndCarriesUserId() {
        var cookie = jwtProvider.generateToken(42L);

        assertEquals("token", cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertTrue(jwtProvider.isValid(cookie.getValue()));
        assertEquals(42L, jwtProvider.getUserId(cookie.getValue()));
    }

    @Test
    void accessToken_usesAccessExpiration() {
        var cookie = jwtProvider.generateToken(42L);

        assertEquals(900L, cookie.getMaxAge().getSeconds());
        Instant expiresAt = JWT.decode(cookie.getValue()).getExpiresAtAsInstant();
        assertTrue(expiresAt.isBefore(Instant.now().plusSeconds(901)));
    }

    @Test
    void refreshToken_isValidOnlyAsRefresh() {
        var refresh = jwtProvider.generateRefreshToken(7L).getValue();

        assertTrue(jwtProvider.isValidRefreshToken(refresh));
        assertEquals(7L, jwtProvider.getUserIdFromRefreshToken(refresh));
        // refresh token não pode ser usado como access token
        assertFalse(jwtProvider.isValid(refresh));
        assertNull(jwtProvider.getUserId(refresh));
    }

    @Test
    void accessToken_cannotBeUsedAsRefresh() {
        var access = jwtProvider.generateToken(7L).getValue();

        assertFalse(jwtProvider.isValidRefreshToken(access));
        assertNull(jwtProvider.getUserIdFromRefreshToken(access));
    }

    @Test
    void tokenSignedWithOtherSecret_isRejected() {
        String forged = JWT.create()
                .withIssuer("Production Review API")
                .withSubject("1")
                .withClaim("type", "access")
                .withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.HMAC256("other-secret"));

        assertFalse(jwtProvider.isValid(forged));
        assertNull(jwtProvider.getUserId(forged));
    }

    @Test
    void expiredToken_isRejected() {
        String expired = JWT.create()
                .withIssuer("Production Review API")
                .withSubject("1")
                .withClaim("type", "access")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256("test-secret"));

        assertFalse(jwtProvider.isValid(expired));
    }

    @Test
    void garbageAndNullTokens_areRejected() {
        assertFalse(jwtProvider.isValid("not-a-jwt"));
        assertFalse(jwtProvider.isValid(null));
        assertNull(jwtProvider.getUserId(""));
    }

    @Test
    void readsTokensFromCookies() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", "abc"), new Cookie("refresh_token", "def"));

        assertEquals("abc", jwtProvider.getTokenFromCookie(request));
        assertEquals("def", jwtProvider.getRefreshTokenFromCookie(request));
        assertNull(jwtProvider.getTokenFromCookie(new MockHttpServletRequest()));
    }

    @Test
    void cleanCookies_expireImmediately() {
        assertEquals(0, jwtProvider.cleanToken().getMaxAge().getSeconds());
        assertEquals(0, jwtProvider.cleanRefreshToken().getMaxAge().getSeconds());
    }
}
