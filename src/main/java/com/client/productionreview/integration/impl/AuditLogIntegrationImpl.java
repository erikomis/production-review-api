package com.client.productionreview.integration.impl;

import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.integration.AuditLogIntegration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;

@Slf4j
@Component
public class AuditLogIntegrationImpl implements AuditLogIntegration {

    public static final String UNAVAILABLE = "Serviço de auditoria indisponível";

    static final String TOKEN_HEADER = "X-Internal-Token";

    static final Set<String> LOG_PARAMS = Set.of("page", "size", "type", "entityType", "userId", "search", "from", "to");

    static final Set<String> SUMMARY_PARAMS = Set.of("from", "to");

    private final RestClient restClient;

    private final String token;

    public AuditLogIntegrationImpl(@Qualifier("logsRestClient") RestClient restClient,
                                   @Value("${logs.api.token:}") String token) {
        this.restClient = restClient;
        this.token = token;
    }

    @Override
    public ResponseEntity<String> getLogs(MultiValueMap<String, String> params) {
        return forward("/api/v1/logs", filter(params, LOG_PARAMS));
    }

    @Override
    public ResponseEntity<String> getSummary(MultiValueMap<String, String> params) {
        return forward("/api/v1/logs/summary", filter(params, SUMMARY_PARAMS));
    }

    private ResponseEntity<String> forward(String path, MultiValueMap<String, String> params) {
        try {
            String body = restClient.get()
                    .uri(uri -> uri.path(path).queryParams(params).build())
                    .header(TOKEN_HEADER, token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
        } catch (RestClientResponseException e) {
            // parâmetro inválido é erro do cliente; o resto (token errado, 5xx) é indisponibilidade
            if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
            }
            log.warn("Serviço de logs respondeu {} em {}", e.getStatusCode().value(), path);
            throw new GlobalException(UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientException e) {
            log.warn("Serviço de logs indisponível: {}", e.getMessage());
            throw new GlobalException(UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static MultiValueMap<String, String> filter(MultiValueMap<String, String> params, Set<String> allowed) {
        MultiValueMap<String, String> filtered = new LinkedMultiValueMap<>();
        if (params != null) {
            params.forEach((key, values) -> {
                if (allowed.contains(key) && values != null) {
                    values.stream().filter(value -> value != null && !value.isBlank())
                            .forEach(value -> filtered.add(key, value));
                }
            });
        }
        return filtered;
    }
}
