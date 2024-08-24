package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.CategoryMapper;
import com.client.productionreview.dtos.category.CategoryRequestDTO;
import com.client.productionreview.dtos.category.CategoryResponseDTO;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private CategoryMapper categoryMapper;


    @Test
    void addCategory_shouldReturnCreatedCategory() throws Exception {
        // Given
        CategoryRequestDTO categoryRequest = new CategoryRequestDTO();
        Category category = new Category();
        CategoryResponseDTO categoryResponse = new CategoryResponseDTO();
        categoryResponse.setName("Electronics");

        Mockito.when(categoryMapper.toModel(any(CategoryRequestDTO.class))).thenReturn(category);
        Mockito.when(categoryService.addCatogory(any(Category.class))).thenReturn(category);
        Mockito.when(categoryMapper.toDTO(any(Category.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/category/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Electronics\" }"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void updateCategory_shouldReturnUpdatedCategory() throws Exception {
        // Given
        Long id = 1L;
        CategoryRequestDTO categoryRequest = new CategoryRequestDTO();
        Category category = new Category();
        CategoryResponseDTO categoryResponse = new CategoryResponseDTO();
        categoryResponse.setName("Home Appliances");

        Mockito.when(categoryMapper.toModel(any(CategoryRequestDTO.class))).thenReturn(category);
        Mockito.when(categoryService.updateCatogory(any(Category.class), anyLong())).thenReturn(category);
        Mockito.when(categoryMapper.toDTO(any(Category.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/category/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Home Appliances\" }"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Home Appliances"));
    }

    @Test
    void deleteCategory_shouldReturnNoContent() throws Exception {
        // Given
        Long id = 1L;

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/category/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCategory_shouldReturnCategoryById() throws Exception {
        // Given
        Long id = 1L;
        Category category = new Category();
        CategoryResponseDTO categoryResponse = new CategoryResponseDTO();
        categoryResponse.setName("Electronics");

        Mockito.when(categoryService.getCatogory(anyLong())).thenReturn(category);
        Mockito.when(categoryMapper.toDTO(any(Category.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/category/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getAllCategories_shouldReturnListOfCategories() throws Exception {
        // Given
        List<Category> categories = List.of(new Category());
        Mockito.when(categoryService.getAllCatogories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/category"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}