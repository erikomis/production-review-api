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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewModerationService reviewModerationService;

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
        when(reviewModerationService.listReviews(eq(ReviewStatus.HIDDEN), eq(1L), eq(3L), eq("spam"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ReviewResponseDTO.builder().id(7L).status(ReviewStatus.HIDDEN)
                        .moderationReason("spam").moderatedByName("Administrador").build())));

        mockMvc.perform(get("/api/v1/admin/reviews").param("status", "HIDDEN").param("note", "1")
                        .param("productId", "3").param("search", "spam").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$.content[0].moderatedByName").value("Administrador"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewModerationService).listReviews(any(), any(), any(), any(), pageable.capture());
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
}
