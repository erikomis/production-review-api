package com.client.productionreview.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

/** Proxy para a API interna do serviço de logs (production-review-api-logs). */
public interface AuditLogIntegration {

    /** GET /api/v1/logs; a resposta JSON é repassada sem alteração. */
    ResponseEntity<String> getLogs(MultiValueMap<String, String> params);

    /** GET /api/v1/logs/summary. */
    ResponseEntity<String> getSummary(MultiValueMap<String, String> params);
}
