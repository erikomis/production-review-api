package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ReviewMapper;
import com.client.productionreview.dtos.review.HelpfulResponseDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(ReviewMapper.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class ReviewControllerTest {

    private static final String VALID_BODY = "{\"title\":\"Bom\",\"description\":\"Gostei\",\"note\":5,\"productId\":2}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private com.client.productionreview.service.ReviewImageService reviewImageService;

    @MockBean
    private com.client.productionreview.service.ReviewReportService reviewReportService;

    private final User user = User.builder().id(10L).name("John").build();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private Review review() {
        return Review.builder().id(1L).title("Bom").description("Gostei").note(5L).productId(2L).userId(10L).build();
    }

    @Test
    void create_usesAuthenticatedUser() throws Exception {
        when(reviewService.saveReview(any(Review.class), eq("John"))).thenReturn(review());

        mockMvc.perform(post("/api/v1/review/")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewService).saveReview(captor.capture(), eq("John"));
        assertEquals(10L, captor.getValue().getUserId());
    }

    @Test
    void create_noteOutOfRange_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/review/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bom\",\"description\":\"Gostei\",\"note\":10,\"productId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("note: Note must be between 1 and 5"));

        verifyNoInteractions(reviewService);
    }

    @Test
    void create_descriptionLongerThanColumn_shouldReturnBadRequest() throws Exception {
        String longText = "a".repeat(256);

        // a coluna description é CHAR(255); antes o texto chegava ao banco e falhava lá
        mockMvc.perform(post("/api/v1/review/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bom\",\"description\":\"" + longText + "\",\"note\":5,\"productId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("description: Description must have at most 255 characters"));

        verifyNoInteractions(reviewService);
    }

    @Test
    void update_passesAuthenticatedUserNotReviewIdAsOwner() throws Exception {
        when(reviewService.updateReview(any(Review.class), eq(1L), eq(user))).thenReturn(review());

        mockMvc.perform(put("/api/v1/review/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        // antes: o id da review era passado como userId para o mapper
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewService).updateReview(captor.capture(), eq(1L), eq(user));
        assertEquals(10L, captor.getValue().getUserId());
    }

    @Test
    void update_byOtherUser_shouldReturnForbidden() throws Exception {
        when(reviewService.updateReview(any(Review.class), eq(1L), any()))
                .thenThrow(new GlobalException("Você não tem permissão para alterar esta review", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/v1/review/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/review/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L, user);
    }

    @Test
    void list_isPaginatedAndNewestFirst() throws Exception {
        var dto = ReviewResponseDTO.builder().id(1L).title("Bom").productName("Phone").userName("John").build();
        when(reviewService.getReviews(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/review/list").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Bom"))
                .andExpect(jsonPath("$.content[0].productName").value("Phone"))
                .andExpect(jsonPath("$.content[0].userName").value("John"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getReviews(captor.capture());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void listByProduct_passesNoteAndSort() throws Exception {
        when(reviewService.getReviewsByProduct(eq(2L), eq(4L), eq("helpful"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ReviewResponseDTO.builder().id(1L).helpfulCount(3).helpfulByMe(true)
                        .status(ReviewStatus.VISIBLE).productSlug("phone").build())));

        mockMvc.perform(get("/api/v1/review/product/{productId}", 2L)
                        .param("note", "4").param("sort", "helpful").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].helpfulCount").value(3))
                .andExpect(jsonPath("$.content[0].helpfulByMe").value(true))
                .andExpect(jsonPath("$.content[0].status").value("VISIBLE"))
                .andExpect(jsonPath("$.content[0].productSlug").value("phone"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getReviewsByProduct(eq(2L), eq(4L), eq("helpful"), captor.capture());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void listByProduct_invalidSort_returnsBadRequest() throws Exception {
        when(reviewService.getReviewsByProduct(eq(2L), any(), eq("random"), any(Pageable.class)))
                .thenThrow(new BadRequestException("Ordenação inválida: random"));

        mockMvc.perform(get("/api/v1/review/product/{productId}", 2L).param("sort", "random"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ordenação inválida: random"));
    }

    @Test
    void me_returnsReviewsOfAuthenticatedUserIncludingHidden() throws Exception {
        when(reviewService.getMyReviews(eq(10L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                ReviewResponseDTO.builder().id(7L).status(ReviewStatus.HIDDEN).moderationReason("spam").build())));

        mockMvc.perform(get("/api/v1/review/me").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$.content[0].moderationReason").value("spam"));
    }

    @Test
    void helpful_togglesForAuthenticatedUser() throws Exception {
        when(reviewService.toggleHelpful(1L, user)).thenReturn(new HelpfulResponseDTO(1L, 4L, true));

        mockMvc.perform(post("/api/v1/review/{id}/helpful", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.helpfulCount").value(4))
                .andExpect(jsonPath("$.helpfulByMe").value(true));
    }

    @Test
    void helpful_ownReview_returnsBadRequest() throws Exception {
        when(reviewService.toggleHelpful(1L, user)).thenThrow(new BadRequestException("própria"));

        mockMvc.perform(post("/api/v1/review/{id}/helpful", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void helpful_hiddenReview_returnsNotFound() throws Exception {
        when(reviewService.toggleHelpful(1L, user)).thenThrow(new NotFoundException("Review not found"));

        mockMvc.perform(post("/api/v1/review/{id}/helpful", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_returnsDetails() throws Exception {
        when(reviewService.getReviewDetails(1L)).thenReturn(ReviewResponseDTO.builder().id(1L).productName("Phone")
                .userName("John").helpfulCount(2).build());

        mockMvc.perform(get("/api/v1/review/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Phone"))
                .andExpect(jsonPath("$.helpfulCount").value(2))
                .andExpect(jsonPath("$.helpfulByMe").value(false));
    }

    @Test
    void summary_includesDistribution() throws Exception {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("1", 0L);
        distribution.put("2", 1L);
        distribution.put("3", 0L);
        distribution.put("4", 2L);
        distribution.put("5", 5L);
        when(reviewService.getProductSummary(2L)).thenReturn(new ReviewSummaryDTO(2L, 8L, 4.4, distribution));

        mockMvc.perform(get("/api/v1/review/product/{productId}/summary", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution.1").value(0))
                .andExpect(jsonPath("$.distribution.5").value(5));
    }

    @Test
    void summary_returnsAverage() throws Exception {
        when(reviewService.getProductSummary(2L)).thenReturn(new ReviewSummaryDTO(2L, 3L, 4.3));

        mockMvc.perform(get("/api/v1/review/product/{productId}/summary", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(3))
                .andExpect(jsonPath("$.averageNote").value(4.3));
    }

    // ---------- fase 3: fotos, denúncia e campos novos ----------

    @Test
    void addImage_returns201WithRelativeUrl() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{1, 2});
        when(reviewImageService.addImage(eq(12L), any(), eq(user)))
                .thenReturn(new com.client.productionreview.dtos.review.ReviewImageDTO(7L, "/api/v1/files/reviews/12/u.jpg"));

        mockMvc.perform(multipart("/api/v1/review/{id}/images", 12L).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.url").value("/api/v1/files/reviews/12/u.jpg"));
    }

    @Test
    void addImage_errorsFromService() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "foto.gif", "image/gif", new byte[]{1});
        when(reviewImageService.addImage(eq(12L), any(), any()))
                .thenThrow(new com.client.productionreview.exception.BadRequestException("Limite de 3 fotos por avaliação"))
                .thenThrow(new com.client.productionreview.exception.GlobalException("Só o autor pode adicionar fotos à avaliação",
                        org.springframework.http.HttpStatus.FORBIDDEN));

        mockMvc.perform(multipart("/api/v1/review/{id}/images", 12L).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Limite de 3 fotos por avaliação"));
        mockMvc.perform(multipart("/api/v1/review/{id}/images", 12L).file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void addImage_withoutFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/review/{id}/images", 12L))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewImageService);
    }

    @Test
    void deleteImage_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/review/{id}/images/{imageId}", 12L, 7L))
                .andExpect(status().isNoContent());

        verify(reviewImageService).deleteImage(12L, 7L, user);
    }

    @Test
    void report_returns201() throws Exception {
        when(reviewReportService.report(eq(12L), any(), eq(user))).thenReturn(
                com.client.productionreview.dtos.review.ReviewReportDTO.builder().id(5L)
                        .reason(com.client.productionreview.model.jpa.ReportReason.SPAM).reporterName("John")
                        .createdAt(java.time.Instant.parse("2026-10-02T02:14:49Z")).build());

        mockMvc.perform(post("/api/v1/review/{id}/report", 12L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\",\"details\":\"link suspeito\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.reason").value("SPAM"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-02T02:14:49Z"));
    }

    @Test
    void report_validation() throws Exception {
        mockMvc.perform(post("/api/v1/review/{id}/report", 12L).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reason: O motivo é obrigatório"));
        mockMvc.perform(post("/api/v1/review/{id}/report", 12L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\",\"details\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("details: Os detalhes devem ter no máximo 500 caracteres"));
        mockMvc.perform(post("/api/v1/review/{id}/report", 12L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CHATO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewReportService);
    }

    @Test
    void report_conflict_returns409() throws Exception {
        when(reviewReportService.report(eq(12L), any(), any()))
                .thenThrow(new com.client.productionreview.exception.BusinessExcepion("Você já denunciou esta avaliação"));

        mockMvc.perform(post("/api/v1/review/{id}/report", 12L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"OTHER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Você já denunciou esta avaliação"));
    }

    @Test
    void reviewResponse_hasPhase3FieldsAndUtcDates() throws Exception {
        ReviewResponseDTO dto = ReviewResponseDTO.builder().id(12L).userUsername("john").reportedByMe(true)
                .createdAt(java.time.Instant.parse("2026-10-02T02:14:49.987Z"))
                .images(List.of(new com.client.productionreview.dtos.review.ReviewImageDTO(7L, "/api/v1/files/reviews/12/u.jpg")))
                .reply(new com.client.productionreview.dtos.review.ReviewReplyDTO("Obrigado", "Equipe",
                        java.time.Instant.parse("2026-10-02T03:00:00Z")))
                .build();
        when(reviewService.getReviewDetails(12L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/review/{id}", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").value("2026-10-02T02:14:49Z"))
                .andExpect(jsonPath("$.userUsername").value("john"))
                .andExpect(jsonPath("$.reportedByMe").value(true))
                .andExpect(jsonPath("$.images[0].url").value("/api/v1/files/reviews/12/u.jpg"))
                .andExpect(jsonPath("$.reply.text").value("Obrigado"))
                .andExpect(jsonPath("$.reply.authorName").value("Equipe"))
                .andExpect(jsonPath("$.reply.repliedAt").value("2026-10-02T03:00:00Z"));
    }
}
