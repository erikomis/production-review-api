package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ReviewMapper;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.exception.GlobalException;
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

import java.util.List;

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
    void listByProduct_defaultsToNewestFirst() throws Exception {
        when(reviewService.getReviewsByProduct(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ReviewResponseDTO.builder().id(1L).build())));

        mockMvc.perform(get("/api/v1/review/product/{productId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getReviewsByProduct(eq(2L), captor.capture());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void summary_returnsAverage() throws Exception {
        when(reviewService.getProductSummary(2L)).thenReturn(new ReviewSummaryDTO(2L, 3L, 4.3));

        mockMvc.perform(get("/api/v1/review/product/{productId}/summary", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(3))
                .andExpect(jsonPath("$.averageNote").value(4.3));
    }
}
