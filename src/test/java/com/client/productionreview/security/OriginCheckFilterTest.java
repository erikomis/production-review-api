package com.client.productionreview.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OriginCheckFilterTest {

    private final OriginCheckFilter filter = new OriginCheckFilter(List.of("http://localhost:5173", "https://projetos-web.com"));

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServerName("api.local");
        request.setServerPort(8084);
        return request;
    }

    @Test
    void post_fromUnknownOrigin_returns403InApiFormat() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/review/");
        request.addHeader("Origin", "https://evil.example.com");

        MockHttpServletResponse response = run(request);

        assertEquals(403, response.getStatus());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"message\":\"Origem não permitida\""), body);
        assertTrue(body.contains("\"httpStatus\":\"FORBIDDEN\""));
        assertTrue(body.contains("\"statusCode\":403"));
    }

    @Test
    void unsafeMethods_fromAllowedOrigin_pass() throws Exception {
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            MockHttpServletRequest request = request(method, "/api/v1/review/1");
            request.addHeader("Origin", "http://localhost:5173");
            assertEquals(200, run(request).getStatus(), method);
        }
    }

    @Test
    void withoutOriginAndReferer_passes() throws Exception {
        assertEquals(200, run(request("POST", "/api/v1/auth/sign-in")).getStatus());
    }

    @Test
    void refererIsUsedWhenOriginIsMissing() throws Exception {
        MockHttpServletRequest allowed = request("POST", "/api/v1/auth/sign-in");
        allowed.addHeader("Referer", "https://projetos-web.com/login?next=/");
        assertEquals(200, run(allowed).getStatus());

        MockHttpServletRequest denied = request("POST", "/api/v1/auth/sign-in");
        denied.addHeader("Referer", "https://evil.example.com/page");
        assertEquals(403, run(denied).getStatus());

        MockHttpServletRequest invalid = request("POST", "/api/v1/auth/sign-in");
        invalid.addHeader("Referer", "not a url");
        assertEquals(403, run(invalid).getStatus());
    }

    @Test
    void nullOrigin_isRejected() throws Exception {
        MockHttpServletRequest request = request("DELETE", "/api/v1/review/1");
        request.addHeader("Origin", "null");
        assertEquals(403, run(request).getStatus());
    }

    @Test
    void safeMethods_andOtherPaths_areNotChecked() throws Exception {
        MockHttpServletRequest get = request("GET", "/api/v1/production/list");
        get.addHeader("Origin", "https://evil.example.com");
        assertEquals(200, run(get).getStatus());

        MockHttpServletRequest actuator = request("POST", "/actuator/refresh");
        actuator.addHeader("Origin", "https://evil.example.com");
        assertEquals(200, run(actuator).getStatus());
    }

    @Test
    void sameOriginAsTheApi_passes() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/review/");
        request.addHeader("Origin", "http://api.local:8084");
        assertEquals(200, run(request).getStatus());
    }
}
