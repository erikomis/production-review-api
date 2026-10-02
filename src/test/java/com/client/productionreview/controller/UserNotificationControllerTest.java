package com.client.productionreview.controller;

import com.client.productionreview.dtos.notification.NotificationResponseDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.NotificationType;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.NotificationService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class UserNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

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
    void list_returnsPageInApiFormat() throws Exception {
        when(notificationService.list(eq(2L), eq(true), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                NotificationResponseDTO.builder().id(1L).type(NotificationType.REVIEW_REPLIED).title("t").message("m")
                        .link("/products/cafe#review-12").read(false).createdAt(Instant.parse("2026-10-02T02:14:49Z")).build())));

        mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", "true").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("REVIEW_REPLIED"))
                .andExpect(jsonPath("$.content[0].link").value("/products/cafe#review-12"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-02T02:14:49Z"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService).list(eq(2L), eq(true), pageable.capture());
        assertEquals(5, pageable.getValue().getPageSize());
    }

    @Test
    void list_defaultsToAll() throws Exception {
        when(notificationService.list(anyLong(), anyBoolean(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isOk());

        verify(notificationService).list(eq(2L), eq(false), any());
    }

    @Test
    void unreadCount() throws Exception {
        when(notificationService.unreadCount(2L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"count\":3}"));
    }

    @Test
    void markRead_andReadAll_return204() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", 9L)).andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/notifications/read-all")).andExpect(status().isNoContent());

        verify(notificationService).markRead(9L, 2L);
        verify(notificationService).markAllRead(2L);
    }

    @Test
    void markRead_notMine_returns404() throws Exception {
        doThrow(new NotFoundException("Notificação não encontrada")).when(notificationService).markRead(9L, 2L);

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", 9L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notificação não encontrada"));
    }
}
