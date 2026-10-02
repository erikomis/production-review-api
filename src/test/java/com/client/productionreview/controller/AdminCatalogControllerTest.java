package com.client.productionreview.controller;

import com.client.productionreview.dtos.admin.DeduplicationResultDTO;
import com.client.productionreview.service.CatalogDeduplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AdminCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogDeduplicationService deduplicationService;

    @Test
    void deduplicate_returnsSummary() throws Exception {
        when(deduplicationService.deduplicate(any())).thenReturn(DeduplicationResultDTO.builder()
                .groups(3).removed(3).keptIds(List.of(1L, 4L, 7L)).removedIds(List.of(2L, 5L, 8L)).build());

        mockMvc.perform(post("/api/v1/admin/catalog/deduplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups").value(3))
                .andExpect(jsonPath("$.removed").value(3))
                .andExpect(jsonPath("$.keptIds[2]").value(7))
                .andExpect(jsonPath("$.removedIds.length()").value(3));
    }
}
