package com.client.productionreview.controller;

import com.client.productionreview.dtos.review.ReviewModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ReviewModerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewModerationService reviewModerationService;

    @MockBean
    private com.client.productionreview.service.ReviewReportService reviewReportService;

    @MockBean
    private com.client.productionreview.service.AdminExportService adminExportService;

    private final User admin = User.builder().id(1L).name("Administrador").build();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_passesFilters() throws Exception {
        // fase 3: a listagem ganhou o filtro "reported" (nulo quando não informado)
        when(reviewModerationService.listReviews(eq(ReviewStatus.HIDDEN), eq(1L), eq(3L), eq("spam"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ReviewResponseDTO.builder().id(7L).status(ReviewStatus.HIDDEN)
                        .moderationReason("spam").moderatedByName("Administrador").build())));

        mockMvc.perform(get("/api/v1/admin/reviews").param("status", "HIDDEN").param("note", "1")
                        .param("productId", "3").param("search", "spam").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$.content[0].moderatedByName").value("Administrador"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewModerationService).listReviews(any(), any(), any(), any(), any(), pageable.capture());
        assertEquals(20, pageable.getValue().getPageSize());
    }

    @Test
    void list_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews").param("status", "DELETED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderate_hidesWithReason() throws Exception {
        when(reviewModerationService.moderate(eq(7L), any(ReviewModerationRequestDTO.class), eq(admin)))
                .thenReturn(ReviewResponseDTO.builder().id(7L).status(ReviewStatus.HIDDEN).moderationReason("spam").build());

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderation", 7L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIDDEN\",\"reason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.moderationReason").value("spam"));

        ArgumentCaptor<ReviewModerationRequestDTO> body = ArgumentCaptor.forClass(ReviewModerationRequestDTO.class);
        verify(reviewModerationService).moderate(eq(7L), body.capture(), eq(admin));
        assertEquals(ReviewStatus.HIDDEN, body.getValue().getStatus());
        assertEquals("spam", body.getValue().getReason());
    }

    @Test
    void moderate_validation() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderation", 7L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status: Status is required"));
        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderation", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\",\"reason\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reviewModerationService);
    }

    @Test
    void moderate_hideWithoutReason_returns400() throws Exception {
        when(reviewModerationService.moderate(eq(7L), any(), any()))
                .thenThrow(new BadRequestException("reason: O motivo é obrigatório para ocultar uma avaliação"));

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderation", 7L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- fase 3 ----------

    @Test
    void list_reportedFilter_andReportsCount() throws Exception {
        when(reviewModerationService.listReviews(any(), any(), any(), any(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ReviewResponseDTO.builder().id(7L).reportsCount(2).build())));

        mockMvc.perform(get("/api/v1/admin/reviews").param("reported", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reportsCount").value(2));
    }

    @Test
    void exportCsv_returnsAttachmentWithBom() throws Exception {
        byte[] csv = com.client.productionreview.utils.CsvWriter.write(List.of("ID"), List.of(List.of(7L)));
        when(adminExportService.reviewsCsv(ReviewStatus.HIDDEN, null, null, "spam", true)).thenReturn(csv);

        mockMvc.perform(get("/api/v1/admin/reviews/export.csv").param("status", "HIDDEN").param("search", "spam")
                        .param("reported", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.matchesPattern(
                        "attachment; filename=\"avaliacoes-\\d{4}-\\d{2}-\\d{2}\\.csv\"")))
                .andExpect(content().bytes(csv));
    }

    @Test
    void bulkModeration_returnsUpdatedCount() throws Exception {
        when(reviewModerationService.moderateBulk(any(), eq(admin))).thenReturn(5);

        mockMvc.perform(patch("/api/v1/admin/reviews/moderation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2,3,4,5],\"status\":\"HIDDEN\",\"reason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(5));
    }

    @Test
    void bulkModeration_validation() throws Exception {
        String tooMany = java.util.stream.LongStream.rangeClosed(1, 101).mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(patch("/api/v1/admin/reviews/moderation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[],\"status\":\"VISIBLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ids: Informe de 1 a 100 avaliações"));
        mockMvc.perform(patch("/api/v1/admin/reviews/moderation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + tooMany + "],\"status\":\"VISIBLE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/admin/reviews/moderation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status: Status is required"));
        verifyNoInteractions(reviewModerationService);
    }

    @Test
    void reports_listAndDismiss() throws Exception {
        when(reviewReportService.listReports(7L)).thenReturn(List.of(com.client.productionreview.dtos.review.ReviewReportDTO
                .builder().id(1L).reason(com.client.productionreview.model.jpa.ReportReason.OFFENSIVE).details("x")
                .reporterName("Maria").createdAt(java.time.Instant.parse("2026-10-02T02:14:49Z")).build()));

        mockMvc.perform(get("/api/v1/admin/reviews/{id}/reports", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("OFFENSIVE"))
                .andExpect(jsonPath("$[0].reporterName").value("Maria"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-02T02:14:49Z"));

        mockMvc.perform(delete("/api/v1/admin/reviews/{id}/reports", 7L))
                .andExpect(status().isNoContent());
        verify(reviewReportService).dismissReports(7L, admin);
    }

    @Test
    void reply_putAndDelete() throws Exception {
        when(reviewModerationService.reply(7L, "Obrigado!", admin)).thenReturn(ReviewResponseDTO.builder().id(7L)
                .reply(new com.client.productionreview.dtos.review.ReviewReplyDTO("Obrigado!", "Administrador",
                        java.time.Instant.parse("2026-10-02T02:14:49Z"))).build());

        mockMvc.perform(put("/api/v1/admin/reviews/{id}/reply", 7L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Obrigado!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply.text").value("Obrigado!"))
                .andExpect(jsonPath("$.reply.authorName").value("Administrador"));

        mockMvc.perform(delete("/api/v1/admin/reviews/{id}/reply", 7L))
                .andExpect(status().isNoContent());
        verify(reviewModerationService).deleteReply(7L, admin);
    }

    @Test
    void reply_validation() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/{id}/reply", 7L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("text: O texto da resposta é obrigatório"));
        mockMvc.perform(put("/api/v1/admin/reviews/{id}/reply", 7L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("text: A resposta deve ter no máximo 1000 caracteres"));
        verifyNoInteractions(reviewModerationService);
    }
}
