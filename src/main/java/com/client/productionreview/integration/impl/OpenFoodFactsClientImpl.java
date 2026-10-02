package com.client.productionreview.integration.impl;

import com.client.productionreview.dtos.importer.OpenFoodFactsProduct;
import com.client.productionreview.dtos.importer.OpenFoodFactsSearchResponse;
import com.client.productionreview.integration.OpenFoodFactsClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.List;

@Slf4j
@Component
public class OpenFoodFactsClientImpl implements OpenFoodFactsClient {

    public static final String USER_AGENT = "ProductionReview/1.0 (production-review-api)";

    static final String FIELDS = "code,product_name,product_name_pt,brands,image_front_url,generic_name_pt,quantity,nutriscore_grade";

    /** Permite trocar o sleep nos testes. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final long minIntervalMs;
    private final long retryDelayMs;
    private final Sleeper sleeper;

    private long lastRequestAt = 0L;

    @Autowired
    public OpenFoodFactsClientImpl(@Qualifier("openFoodFactsRestClient") RestClient restClient, ObjectMapper objectMapper,
                                   @Value("${openfoodfacts.min-interval-ms:6500}") long minIntervalMs,
                                   @Value("${openfoodfacts.retry-delay-ms:15000}") long retryDelayMs) {
        this(restClient, objectMapper, minIntervalMs, retryDelayMs, Thread::sleep);
    }

    public OpenFoodFactsClientImpl(RestClient restClient, ObjectMapper objectMapper, long minIntervalMs, long retryDelayMs,
                                   Sleeper sleeper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.minIntervalMs = minIntervalMs;
        this.retryDelayMs = retryDelayMs;
        this.sleeper = sleeper;
    }

    @Override
    public synchronized List<OpenFoodFactsProduct> searchProducts(String categoryTag, int pageSize) {
        try {
            return attempt(categoryTag, pageSize);
        } catch (RetryableException first) {
            log.warn("Open Food Facts ({}): {}; nova tentativa em {} ms", categoryTag, first.getMessage(), retryDelayMs);
            sleep(retryDelayMs);
            try {
                return attempt(categoryTag, pageSize);
            } catch (RetryableException second) {
                throw new OpenFoodFactsException(second.getMessage());
            }
        }
    }

    private List<OpenFoodFactsProduct> attempt(String categoryTag, int pageSize) {
        throttle();
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/api/v2/search")
                            .queryParam("categories_tags_en", categoryTag)
                            .queryParam("countries_tags_en", "brazil")
                            .queryParam("sort_by", "unique_scans_n")
                            .queryParam("page_size", pageSize)
                            .queryParam("fields", FIELDS)
                            .build())
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 429 || status.value() == 503) {
                throw new RetryableException("HTTP " + status.value());
            }
            throw new OpenFoodFactsException("HTTP " + status.value());
        } catch (ResourceAccessException e) {
            throw new OpenFoodFactsException(e.getCause() instanceof SocketTimeoutException ? "timeout" : "falha de conexão");
        } finally {
            lastRequestAt = System.currentTimeMillis();
        }

        try {
            OpenFoodFactsSearchResponse response = objectMapper.readValue(body == null ? "" : body, OpenFoodFactsSearchResponse.class);
            return response.getProducts() == null ? List.of() : response.getProducts();
        } catch (JsonProcessingException e) {
            throw new RetryableException("resposta não-JSON");
        }
    }

    /** Respeita o limite deles (~10 buscas/minuto): intervalo mínimo entre buscas. */
    private void throttle() {
        if (lastRequestAt == 0L || minIntervalMs <= 0) {
            return;
        }
        long wait = lastRequestAt + minIntervalMs - System.currentTimeMillis();
        if (wait > 0) {
            sleep(wait);
        }
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenFoodFactsException("interrompido");
        }
    }

    private static class RetryableException extends RuntimeException {
        RetryableException(String message) {
            super(message);
        }
    }
}
