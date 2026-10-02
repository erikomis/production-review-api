package com.client.productionreview.integration;

import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.integration.impl.AuditLogIntegrationImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AuditLogIntegrationTest {

    private MockRestServiceServer server;
    private AuditLogIntegrationImpl integration;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://logs.test");
        server = MockRestServiceServer.bindTo(builder).build();
        integration = new AuditLogIntegrationImpl(builder.build(), "secret-token");
    }

    @Test
    void getLogs_forwardsAllowedParamsWithToken_andReturnsBodyAsIs() {
        server.expect(requestTo("http://logs.test/api/v1/logs?page=0&size=20&type=REVIEW_CREATED&search=teste"))
                .andExpect(header("X-Internal-Token", "secret-token"))
                .andRespond(withSuccess("{\"content\":[],\"page\":{\"size\":20}}", MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("size", "20");
        params.add("type", "REVIEW_CREATED");
        params.add("search", "teste");
        params.add("hack", "x");
        params.add("from", "");

        ResponseEntity<String> response = integration.getLogs(params);

        server.verify();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("{\"content\":[],\"page\":{\"size\":20}}", response.getBody());
    }

    @Test
    void getSummary_forwardsFromTo() {
        server.expect(requestTo("http://logs.test/api/v1/logs/summary?from=2026-10-01&to=2026-10-02"))
                .andRespond(withSuccess("{\"total\":3}", MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("from", "2026-10-01");
        params.add("to", "2026-10-02");
        params.add("type", "ignored");

        assertEquals("{\"total\":3}", integration.getSummary(params).getBody());
        server.verify();
    }

    @Test
    void badRequestFromLogsService_isPassedThrough() {
        server.expect(requestTo("http://logs.test/api/v1/logs?from=ontem"))
                .andRespond(withBadRequest().body("{\"message\":\"from inválido\"}").contentType(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("from", "ontem");

        ResponseEntity<String> response = integration.getLogs(params);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("{\"message\":\"from inválido\"}", response.getBody());
    }

    @Test
    void serverErrorOrWrongToken_isServiceUnavailable() {
        server.expect(requestTo("http://logs.test/api/v1/logs")).andRespond(withServerError());
        server.expect(requestTo("http://logs.test/api/v1/logs")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        GlobalException first = assertThrows(GlobalException.class, () -> integration.getLogs(new LinkedMultiValueMap<>()));
        GlobalException second = assertThrows(GlobalException.class, () -> integration.getLogs(new LinkedMultiValueMap<>()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, first.getHttpStatus());
        assertEquals("Serviço de auditoria indisponível", first.getMensage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, second.getHttpStatus());
    }

    @Test
    void serviceDown_isServiceUnavailable() {
        AuditLogIntegrationImpl down = new AuditLogIntegrationImpl(RestClient.builder().baseUrl("http://localhost:1").build(), "t");

        GlobalException ex = assertThrows(GlobalException.class, () -> down.getLogs(new LinkedMultiValueMap<>()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }
}
