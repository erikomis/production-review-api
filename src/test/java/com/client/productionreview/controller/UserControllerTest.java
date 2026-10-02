package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.UserMapper;
import com.client.productionreview.dtos.notification.NotificationPreferencesDTO;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ProductFollowService;
import com.client.productionreview.service.UserService;
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

@WebMvcTest(UserController.class)
@Import(UserMapper.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductFollowService productFollowService;

    private final User user = User.builder().id(2L).name("Maria").build();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preferences_getAndPatch() throws Exception {
        when(userService.getPreferences(2L)).thenReturn(new NotificationPreferencesDTO(true));
        when(userService.updatePreferences(2L, false)).thenReturn(new NotificationPreferencesDTO(false));

        mockMvc.perform(get("/api/v1/user/me/preferences"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"emailNotifications\":true}"));
        mockMvc.perform(patch("/api/v1/user/me/preferences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailNotifications\":false}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"emailNotifications\":false}"));
    }

    @Test
    void preferences_patchWithoutValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/user/me/preferences").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("emailNotifications: emailNotifications é obrigatório"));
        verify(userService, never()).updatePreferences(anyLong(), anyBoolean());
    }

    @Test
    void following_returnsProductSummaries() throws Exception {
        ProductSummaryDTO summary = new ProductSummaryDTO();
        summary.setId(3L);
        summary.setName("Café");
        when(productFollowService.following(eq(2L), any())).thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/user/me/following").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Café"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productFollowService).following(eq(2L), pageable.capture());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals("name: ASC", pageable.getValue().getSort().toString());
    }
}
