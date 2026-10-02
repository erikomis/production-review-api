package com.client.productionreview.integration;

import com.client.productionreview.dtos.importer.OpenFoodFactsProduct;
import com.client.productionreview.integration.impl.OpenFoodFactsClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Nenhuma chamada real: o RestClient é ligado ao MockRestServiceServer e o sleep é capturado. */
class OpenFoodFactsClientTest {

    private static final String BODY = "{\"count\":1,\"products\":[{\"code\":\"7891000102626\","
            + "\"product_name_pt\":\"Aveia Em Flocos Finos Nestlé Caixa 170g\",\"brands\":\"Nestlé\",\"quantity\":\"500g\","
            + "\"nutriscore_grade\":\"a\",\"image_front_url\":\"https://images.openfoodfacts.org/front_pt.9.400.jpg\"}]}";

    private MockRestServiceServer server;
    private RestClient restClient;
    private final List<Long> sleeps = new ArrayList<>();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://off.test");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private OpenFoodFactsClientImpl client(long minInterval, long retryDelay) {
        return new OpenFoodFactsClientImpl(restClient, new ObjectMapper(), minInterval, retryDelay, sleeps::add);
    }

    @Test
    void search_sendsExpectedQueryAndUserAgent_andParsesProducts() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("categories_tags_en", "breakfast-cereals"))
                .andExpect(queryParam("countries_tags_en", "brazil"))
                .andExpect(queryParam("sort_by", "unique_scans_n"))
                .andExpect(queryParam("page_size", "24"))
                .andExpect(queryParam("fields", "code,product_name,product_name_pt,brands,image_front_url,generic_name_pt,quantity,nutriscore_grade"))
                .andExpect(header("User-Agent", "ProductionReview/1.0 (production-review-api)"))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        List<OpenFoodFactsProduct> products = client(0, 0).searchProducts("breakfast-cereals", 24);

        server.verify();
        assertEquals(1, products.size());
        assertEquals("7891000102626", products.get(0).getCode());
        assertEquals("Aveia Em Flocos Finos Nestlé Caixa 170g", products.get(0).getProductNamePt());
        assertEquals("a", products.get(0).getNutriscoreGrade());
        assertEquals("https://images.openfoodfacts.org/front_pt.9.400.jpg", products.get(0).getImageFrontUrl());
    }

    @Test
    void search_retriesOnceAfter429() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        List<OpenFoodFactsProduct> products = client(0, 15_000).searchProducts("sodas", 4);

        server.verify();
        assertEquals(1, products.size());
        assertEquals(List.of(15_000L), sleeps);
    }

    @Test
    void search_nonJsonTwice_fails() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withSuccess("<html>busy</html>", MediaType.TEXT_HTML));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        var ex = assertThrows(OpenFoodFactsClient.OpenFoodFactsException.class, () -> client(0, 15_000).searchProducts("sodas", 4));

        server.verify();
        assertEquals("HTTP 503", ex.getMessage());
    }

    @Test
    void search_serverErrorWithoutRetry() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withServerError());

        var ex = assertThrows(OpenFoodFactsClient.OpenFoodFactsException.class, () -> client(0, 15_000).searchProducts("sodas", 4));

        server.verify();
        assertEquals("HTTP 500", ex.getMessage());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void search_waitsMinimumIntervalBetweenSearches() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://off.test/api/v2/search")))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        OpenFoodFactsClientImpl client = client(6_500, 15_000);
        client.searchProducts("sodas", 4);
        client.searchProducts("milks", 4);

        server.verify();
        assertEquals(1, sleeps.size());
        assertTrue(sleeps.get(0) > 6_000 && sleeps.get(0) <= 6_500, "esperou " + sleeps.get(0));
    }
}
