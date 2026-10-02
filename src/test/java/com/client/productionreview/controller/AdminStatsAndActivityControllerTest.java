package com.client.productionreview.controller;

import com.client.productionreview.dtos.admin.StatsDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.integration.AuditLogIntegration;
import com.client.productionreview.service.StatsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminStatsController.class, AdminActivityController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AdminStatsAndActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @MockBean
    private AuditLogIntegration auditLogIntegration;

    // dependência nova do AdminActivityController (exportação CSV)
    @MockBean
    private com.client.productionreview.service.AdminExportService adminExportService;

    @Test
    void stats_defaultsTo30Days() throws Exception {
        when(statsService.getStats(30)).thenReturn(StatsDTO.builder()
                .totals(StatsDTO.Totals.builder().products(3).categories(2).subCategories(3).reviews(3).hiddenReviews(0).users(2).build())
                .averageNote(4.3)
                .ratingDistribution(Map.of("1", 0L, "2", 0L, "3", 1L, "4", 0L, "5", 2L))
                .reviewsPerDay(List.of(new StatsDTO.ReviewsPerDay(LocalDate.of(2026, 9, 3), 0, null)))
                .usersPerDay(List.of(new StatsDTO.UsersPerDay(LocalDate.of(2026, 9, 3), 0)))
                .topProducts(List.of()).topCategories(List.of())
                .build());

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.products").value(3))
                .andExpect(jsonPath("$.totals.hiddenReviews").value(0))
                .andExpect(jsonPath("$.averageNote").value(4.3))
                .andExpect(jsonPath("$.ratingDistribution.5").value(2))
                .andExpect(jsonPath("$.reviewsPerDay[0].date").value("2026-09-03"))
                .andExpect(jsonPath("$.reviewsPerDay[0].averageNote").isEmpty())
                .andExpect(jsonPath("$.usersPerDay[0].count").value(0));
    }

    @Test
    void stats_invalidDays_returns400() throws Exception {
        when(statsService.getStats(400)).thenThrow(new BadRequestException("days deve estar entre 7 e 365"));

        mockMvc.perform(get("/api/v1/admin/stats").param("days", "400"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/stats").param("days", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SuppressWarnings("unchecked")
    void activity_forwardsQueryParams() throws Exception {
        when(auditLogIntegration.getLogs(any())).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON).body("{\"content\":[],\"page\":{\"size\":20,\"number\":0,\"totalElements\":0,\"totalPages\":0}}"));

        mockMvc.perform(get("/api/v1/admin/activity").param("type", "REVIEW_CREATED").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page.size").value(20));

        ArgumentCaptor<MultiValueMap<String, String>> params = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(auditLogIntegration).getLogs(params.capture());
        assertEquals("REVIEW_CREATED", params.getValue().getFirst("type"));
    }

    @Test
    void activitySummary_serviceDown_returns503() throws Exception {
        when(auditLogIntegration.getSummary(any()))
                .thenThrow(new GlobalException("Serviço de auditoria indisponível", HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(get("/api/v1/admin/activity/summary").param("from", "2026-10-01"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Serviço de auditoria indisponível"));
    }

    @Test
    void activityExport_forwardsFilters() throws Exception {
        byte[] csv = com.client.productionreview.utils.CsvWriter.write(java.util.List.of("Data"), java.util.List.of());
        when(adminExportService.activityCsv(any())).thenReturn(csv);

        mockMvc.perform(get("/api/v1/admin/activity/export.csv").param("type", "REVIEW_CREATED"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"atividade-")))
                .andExpect(content().bytes(csv));

        org.mockito.ArgumentCaptor<org.springframework.util.MultiValueMap<String, String>> params =
                org.mockito.ArgumentCaptor.forClass(org.springframework.util.MultiValueMap.class);
        verify(adminExportService).activityCsv(params.capture());
        assertEquals("REVIEW_CREATED", params.getValue().getFirst("type"));
    }
}
