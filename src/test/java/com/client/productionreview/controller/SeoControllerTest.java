package com.client.productionreview.controller;

import com.client.productionreview.dtos.seo.SeoProductDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.service.SeoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class SeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeoService seoService;

    @Test
    void sitemap_isXmlWithCache() throws Exception {
        when(seoService.sitemap()).thenReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset></urlset>\n");

        mockMvc.perform(get("/api/v1/seo/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(header().string("Cache-Control", "max-age=3600, public"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset>")));
    }

    @Test
    void product_returnsJsonLdData() throws Exception {
        when(seoService.product("cafe")).thenReturn(SeoProductDTO.builder().name("Café").description("d").image("https://img")
                .url("http://localhost:5174/products/cafe")
                .aggregateRating(new SeoProductDTO.AggregateRating(4.5, 2))
                .reviews(List.of(new SeoProductDTO.SeoReview("Maria", Instant.parse("2026-10-01T12:00:00Z"), "Bom", "Ótimo", 5L)))
                .build());

        mockMvc.perform(get("/api/v1/seo/products/{slug}", "cafe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Café"))
                .andExpect(jsonPath("$.brand").doesNotExist())
                .andExpect(jsonPath("$.aggregateRating.ratingValue").value(4.5))
                .andExpect(jsonPath("$.aggregateRating.reviewCount").value(2))
                .andExpect(jsonPath("$.reviews[0].author").value("Maria"))
                .andExpect(jsonPath("$.reviews[0].datePublished").value("2026-10-01T12:00:00Z"))
                .andExpect(jsonPath("$.reviews[0].ratingValue").value(5));
    }

    @Test
    void product_withoutRating_hasNullAggregate() throws Exception {
        when(seoService.product("novo")).thenReturn(SeoProductDTO.builder().name("Novo").brand("Marca").build());

        mockMvc.perform(get("/api/v1/seo/products/{slug}", "novo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Marca"))
                .andExpect(jsonPath("$.aggregateRating").isEmpty())
                .andExpect(jsonPath("$.reviews").isArray());
    }

    @Test
    void product_missing_returns404() throws Exception {
        when(seoService.product("x")).thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/seo/products/{slug}", "x")).andExpect(status().isNotFound());
    }
}
