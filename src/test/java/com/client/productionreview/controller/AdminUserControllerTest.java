package com.client.productionreview.controller;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.AdminUserService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    // dependência nova do controller (exportação CSV)
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

    private AdminUserDTO maria() {
        return AdminUserDTO.builder().id(2L).name("Maria").username("maria").email("maria@mail.com").active(true)
                .roles(List.of("USER")).createdAt(Instant.parse("2026-10-01T10:00:00.123456Z")).reviewsCount(3).build();
    }

    @Test
    void list_passesFiltersAndReturnsContract() throws Exception {
        when(adminUserService.listUsers(eq("mar"), eq("USER"), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(maria())));

        mockMvc.perform(get("/api/v1/admin/users").param("search", "mar").param("role", "USER").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].username").value("maria"))
                .andExpect(jsonPath("$.content[0].email").value("maria@mail.com"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].roles[0]").value("USER"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-01T10:00:00Z"))
                .andExpect(jsonPath("$.content[0].reviewsCount").value(3))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(adminUserService).listUsers(any(), any(), any(), pageable.capture());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void setAdmin_andSelfRemovalIs400() throws Exception {
        when(adminUserService.setAdmin(2L, true, admin)).thenReturn(maria());
        when(adminUserService.setAdmin(1L, false, admin)).thenThrow(new BadRequestException("própria"));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/admin", 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"admin\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria"));
        mockMvc.perform(patch("/api/v1/admin/users/{id}/admin", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"admin\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setActive_andValidation() throws Exception {
        when(adminUserService.setActive(2L, false, admin)).thenReturn(maria());

        mockMvc.perform(patch("/api/v1/admin/users/{id}/active", 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/users/{id}/active", 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("active: Active is required"));
    }

    @Test
    void exportCsv_returnsAttachment() throws Exception {
        byte[] csv = com.client.productionreview.utils.CsvWriter.write(List.of("ID"), List.of(List.of(2L)));
        when(adminExportService.usersCsv("mar", "USER", true)).thenReturn(csv);

        mockMvc.perform(get("/api/v1/admin/users/export.csv").param("search", "mar").param("role", "USER").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"usuarios-")))
                .andExpect(content().bytes(csv));
    }
}
