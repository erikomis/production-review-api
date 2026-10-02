package com.client.productionreview.controller;

import com.client.productionreview.dtos.importer.ImportJobDTO;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.CatalogImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AdminImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogImportService catalogImportService;

    private final User admin = User.builder().id(1L).name("Administrador").build();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private ImportJobDTO job(ImportJobDTO.Status status) {
        return ImportJobDTO.builder().id("abc").source("OPEN_FOOD_FACTS").status(status).totalSteps(12).completedSteps(3)
                .currentStep("Bebidas › Cafés").errors(List.of("Laticínios › Queijos: timeout"))
                .startedAt(Instant.parse("2026-10-02T03:10:00Z")).startedBy("Administrador").build();
    }

    @Test
    void start_returns202WithJob() throws Exception {
        when(catalogImportService.startOpenFoodFactsImport(12, admin)).thenReturn(job(ImportJobDTO.Status.RUNNING));

        mockMvc.perform(post("/api/v1/admin/import/open-food-facts")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productsPerSubcategory\":12}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("abc"))
                .andExpect(jsonPath("$.source").value("OPEN_FOOD_FACTS"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.totalSteps").value(12))
                .andExpect(jsonPath("$.currentStep").value("Bebidas › Cafés"))
                .andExpect(jsonPath("$.errors[0]").value("Laticínios › Queijos: timeout"))
                .andExpect(jsonPath("$.startedAt").value("2026-10-02T03:10:00Z"))
                .andExpect(jsonPath("$.finishedAt").isEmpty())
                .andExpect(jsonPath("$.startedBy").value("Administrador"));
    }

    @Test
    void start_withoutBody_usesDefault12() throws Exception {
        when(catalogImportService.startOpenFoodFactsImport(anyInt(), any())).thenReturn(job(ImportJobDTO.Status.RUNNING));

        mockMvc.perform(post("/api/v1/admin/import/open-food-facts"))
                .andExpect(status().isAccepted());

        verify(catalogImportService).startOpenFoodFactsImport(eq(12), any());
    }

    @Test
    void start_outOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import/open-food-facts")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productsPerSubcategory\":31}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/import/open-food-facts")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productsPerSubcategory\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalogImportService);
    }

    @Test
    void start_whileRunning_returns409() throws Exception {
        when(catalogImportService.startOpenFoodFactsImport(anyInt(), any()))
                .thenThrow(new BusinessExcepion("Já existe uma importação em andamento"));

        mockMvc.perform(post("/api/v1/admin/import/open-food-facts")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getJob_and404() throws Exception {
        when(catalogImportService.getJob("abc")).thenReturn(job(ImportJobDTO.Status.COMPLETED));
        when(catalogImportService.getJob("nope")).thenThrow(new NotFoundException("Import job not found"));

        mockMvc.perform(get("/api/v1/admin/import/jobs/{id}", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/admin/import/jobs/{id}", "nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void latest_returnsJobOr204() throws Exception {
        when(catalogImportService.getLatestJob()).thenReturn(Optional.empty(), Optional.of(job(ImportJobDTO.Status.RUNNING)));

        mockMvc.perform(get("/api/v1/admin/import/jobs/latest"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/admin/import/jobs/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc"));
    }
}
