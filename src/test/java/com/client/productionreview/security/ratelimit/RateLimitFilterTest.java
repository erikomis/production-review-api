package com.client.productionreview.security.ratelimit;

import com.client.productionreview.metrics.BusinessMetrics;
import com.client.productionreview.model.jpa.User;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private InMemoryRateLimitStore store;
    private RateLimitProperties properties;
    private SimpleMeterRegistry registry;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        store = new InMemoryRateLimitStore();
        properties = new RateLimitProperties();
        registry = new SimpleMeterRegistry();
        filter = new RateLimitFilter(store, properties, new BusinessMetrics(registry));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest signIn(String ip, String username) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/sign-in");
        request.setRemoteAddr(ip);
        request.setContentType("application/json");
        request.setContent(("{\"username\":\"" + username + "\",\"password\":\"x\"}").getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private MockHttpServletResponse run(MockHttpServletRequest request, MockFilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        return run(request, new MockFilterChain());
    }

    @Test
    void signIn_sixthAttemptWithSameLogin_returns429WithRetryAfter() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, run(signIn("10.0.0.1", "admin")).getStatus());
        }

        MockHttpServletResponse blocked = run(signIn("10.0.0.1", "ADMIN "));

        assertEquals(429, blocked.getStatus());
        long retryAfter = Long.parseLong(blocked.getHeader("Retry-After"));
        assertTrue(retryAfter >= 1 && retryAfter <= 60);
        String body = blocked.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"message\":\"Muitas tentativas. Tente novamente em " + retryAfter + " segundos.\""), body);
        assertTrue(body.contains("\"httpStatus\":\"TOO_MANY_REQUESTS\""));
        assertTrue(body.contains("\"statusCode\":429"));
        assertEquals(1.0, registry.get("reviewstore.rate.limit.rejections").tag("rule", "sign-in-login").counter().count());
    }

    @Test
    void signIn_otherLoginOrOtherIp_isCountedSeparately() throws Exception {
        for (int i = 0; i < 5; i++) {
            run(signIn("10.0.0.1", "admin"));
        }
        assertEquals(200, run(signIn("10.0.0.1", "usuario")).getStatus());
        assertEquals(200, run(signIn("10.0.0.2", "admin")).getStatus());
    }

    @Test
    void signIn_ipLimit_appliesAcrossLogins() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertEquals(200, run(signIn("10.0.0.9", "user" + i)).getStatus());
        }
        assertEquals(429, run(signIn("10.0.0.9", "another")).getStatus());
    }

    @Test
    void bodyIsStillAvailableToTheController() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        run(signIn("10.0.0.1", "admin"), chain);

        ServletRequest forwarded = chain.getRequest();
        String body = new String(forwarded.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("{\"username\":\"admin\",\"password\":\"x\"}", body);
    }

    @Test
    void limitAndWindow_areConfigurable() throws Exception {
        RateLimitProperties.Limit limit = new RateLimitProperties.Limit();
        limit.setLimit(1);
        limit.setWindow(Duration.ofSeconds(30));
        properties.getRules().put("sign-up-ip", limit);

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/auth/sign-up");
        assertEquals(200, run(first).getStatus());
        MockHttpServletResponse second = run(new MockHttpServletRequest("POST", "/api/v1/auth/sign-up"));

        assertEquals(429, second.getStatus());
        assertTrue(Long.parseLong(second.getHeader("Retry-After")) <= 30);
    }

    @Test
    void recoveryCode_getAndPatchShareTheIpCounter() throws Exception {
        for (int i = 0; i < 5; i++) {
            run(new MockHttpServletRequest("GET", "/api/v1/auth/recovery-code"));
            run(new MockHttpServletRequest("PATCH", "/api/v1/auth/recovery-code/password"));
        }
        assertEquals(429, run(new MockHttpServletRequest("GET", "/api/v1/auth/recovery-code")).getStatus());
    }

    @Test
    void recoverySend_limitsByEmail() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/send-recovery-code/send");
            request.setRemoteAddr("10.0.0." + i);
            request.setContent("{\"email\":\"a@b.com\"}".getBytes(StandardCharsets.UTF_8));
            assertEquals(200, run(request).getStatus());
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/send-recovery-code/send");
        request.setRemoteAddr("10.0.0.99");
        request.setContent("{\"email\":\"A@B.com\"}".getBytes(StandardCharsets.UTF_8));
        assertEquals(429, run(request).getStatus());
    }

    @Test
    void reviewCreation_isLimitedPerUser_andSkippedWithoutLogin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                User.builder().id(5L).build(), null, List.of()));
        for (int i = 0; i < 10; i++) {
            assertEquals(200, run(new MockHttpServletRequest("POST", "/api/v1/review/")).getStatus());
        }
        assertEquals(429, run(new MockHttpServletRequest("POST", "/api/v1/review/")).getStatus());

        SecurityContextHolder.clearContext();
        // sem login a regra não se aplica (o Spring Security responde 401 depois)
        assertEquals(200, run(new MockHttpServletRequest("POST", "/api/v1/review/")).getStatus());
    }

    @Test
    void reportRoute_usesItsOwnRule() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                User.builder().id(5L).build(), null, List.of()));
        for (int i = 0; i < 20; i++) {
            assertEquals(200, run(new MockHttpServletRequest("POST", "/api/v1/review/" + i + "/report")).getStatus());
        }
        assertEquals(429, run(new MockHttpServletRequest("POST", "/api/v1/review/1/report")).getStatus());
    }

    @Test
    void otherRoutes_areNotLimited() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertEquals(200, run(new MockHttpServletRequest("GET", "/api/v1/production/list")).getStatus());
        }
    }

    @Test
    void disabled_letsEverythingThrough() throws Exception {
        properties.setEnabled(false);
        for (int i = 0; i < 10; i++) {
            assertEquals(200, run(signIn("10.0.0.1", "admin")).getStatus());
        }
    }

    @Test
    void storeFailure_doesNotBlockRequests() throws Exception {
        RateLimitStore broken = mock(RateLimitStore.class);
        when(broken.hit(any(), any())).thenThrow(new IllegalStateException("redis fora do ar"));
        filter = new RateLimitFilter(broken, properties, new BusinessMetrics(registry));

        assertEquals(200, run(signIn("10.0.0.1", "admin")).getStatus());
    }
}
