package com.client.productionreview.controller;


import com.client.productionreview.controller.mapper.SubCategoryMapper;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.service.SubCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(SubCategoriaController.class)
@Import(SubCategoryMapper.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
public class SubCategorieControllerTest {

    private static final String VALID_BODY = "{\"name\":\"Celulares\",\"description\":\"d\",\"slug\":\"celulares\",\"categorieId\":3}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubCategoryService subCategoriaService;

    private SubCategory subCategory() {
        return SubCategory.builder().id(1L).name("Celulares").description("d").slug("celulares").categorieId(3L).build();
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        when(subCategoriaService.addSubCategory(any(SubCategory.class))).thenReturn(subCategory());

        mockMvc.perform(post("/api/v1/sub-categorie/create").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categorieId").value(3));
    }

    @Test
    void create_withoutCategoryId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/sub-categorie/create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Celulares\",\"description\":\"d\",\"slug\":\"celulares\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("categorieId: CategorieId is required"));
    }

    @Test
    void create_withNegativeCategoryId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/sub-categorie/create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Celulares\",\"description\":\"d\",\"slug\":\"celulares\",\"categorieId\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturnOk() throws Exception {
        when(subCategoriaService.updateSubCategory(any(SubCategory.class), eq(1L))).thenReturn(subCategory());

        mockMvc.perform(put("/api/v1/sub-categorie/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Celulares"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/sub-categorie/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(subCategoriaService).deleteSubCategory(1L);
    }

    @Test
    void get_shouldReturnSubCategory() throws Exception {
        when(subCategoriaService.getSubCategory(1L)).thenReturn(subCategory());

        mockMvc.perform(get("/api/v1/sub-categorie/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("celulares"));
    }

    @Test
    void list_shouldReturnAll() throws Exception {
        when(subCategoriaService.getAllSubCategorie()).thenReturn(List.of(subCategory()));

        mockMvc.perform(get("/api/v1/sub-categorie/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
