package com.client.productionreview.controller;

import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.user.PublicProfileDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.service.ReviewService;
import com.client.productionreview.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class PublicUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ReviewService reviewService;

    @Test
    void profile_returnsPublicFieldsOnly() throws Exception {
        when(userService.getPublicProfile("maria")).thenReturn(PublicProfileDTO.builder().username("maria").name("Maria")
                .memberSince(Instant.parse("2026-09-01T12:00:00Z")).reviewsCount(3).helpfulReceived(7).averageNoteGiven(4.3)
                .build());

        mockMvc.perform(get("/api/v1/users/{username}", "maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria"))
                .andExpect(jsonPath("$.memberSince").value("2026-09-01T12:00:00Z"))
                .andExpect(jsonPath("$.reviewsCount").value(3))
                .andExpect(jsonPath("$.helpfulReceived").value(7))
                .andExpect(jsonPath("$.averageNoteGiven").value(4.3))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void profile_missing_returns404() throws Exception {
        when(userService.getPublicProfile("x")).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/{username}", "x")).andExpect(status().isNotFound());
    }

    @Test
    void reviews_returnsPage() throws Exception {
        when(reviewService.getUserReviews(eq("maria"), any())).thenReturn(new PageImpl<>(List.of(
                ReviewResponseDTO.builder().id(12L).userUsername("maria").build())));

        mockMvc.perform(get("/api/v1/users/{username}/reviews", "maria").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.content[0].userUsername").value("maria"))
                .andExpect(jsonPath("$.page.size").value(1));
    }
}
