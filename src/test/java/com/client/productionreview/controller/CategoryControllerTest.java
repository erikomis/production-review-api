package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.CategoryMapper;
import com.client.productionreview.dtos.category.CategoryDetailDTO;
import com.client.productionreview.dtos.category.CategoryRequestDTO;
import com.client.productionreview.dtos.category.CategoryResponseDTO;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
        Mockito.when(categoryService.addCategory(any(Category.class))).thenReturn(category);
        Mockito.when(categoryMapper.toDTO(any(Category.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/category/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Electronics\", \"description\": \"Electronic devices\", \"slug\": \"electronics\" }"))
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
        Mockito.when(categoryService.updateCategory(any(Category.class), anyLong())).thenReturn(category);
        Mockito.when(categoryMapper.toDTO(any(Category.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/category/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Home Appliances\", \"description\": \"d\", \"slug\": \"home\" }"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Home Appliances"));
    }

    @Test
    void updateCategory_withMissingFields_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/category/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Home Appliances\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("description: Description is required; slug: Slug is required"));

        Mockito.verifyNoInteractions(categoryService);
    }

    @Test
    void getCategory_notFound_shouldReturn404() throws Exception {
        Mockito.when(categoryService.getCategory(99L)).thenThrow(new NotFoundException("Categorie not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/category/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Categorie not found"));
    }

    @Test
    void addCategory_duplicated_shouldReturnConflict() throws Exception {
        Mockito.when(categoryMapper.toModel(any(CategoryRequestDTO.class))).thenReturn(new Category());
        Mockito.when(categoryService.addCategory(any(Category.class))).thenThrow(new BusinessExcepion("Categorie already exists"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/category/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Electronics\", \"description\": \"d\", \"slug\": \"e\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
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

        Mockito.when(categoryService.getCategory(anyLong())).thenReturn(category);
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
        Mockito.when(categoryService.getAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/category/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getCategoryBySlug_returnsCategoryWithSubCategories() throws Exception {
        Category category = Category.builder().id(1L).name("Bebidas").description("d").slug("bebidas").build();
        CategoryDetailDTO detail = new CategoryDetailDTO(1L, "Bebidas", "d", "bebidas",
                List.of(new CategoryDetailDTO.SubCategoryItem(4L, "Cafés", "c", "cafes")));
        Mockito.when(categoryService.getCategoryBySlug("bebidas")).thenReturn(category);
        Mockito.when(categoryMapper.toDetailDTO(category)).thenReturn(detail);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/category/slug/{slug}", "bebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("bebidas"))
                .andExpect(jsonPath("$.subCategories", hasSize(1)))
                .andExpect(jsonPath("$.subCategories[0].slug").value("cafes"))
                .andExpect(jsonPath("$.subCategories[0].categorieId").doesNotExist());
    }

    @Test
    void getCategoryBySlug_notFound() throws Exception {
        Mockito.when(categoryService.getCategoryBySlug("nope")).thenThrow(new NotFoundException("Categorie not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/category/slug/{slug}", "nope"))
                .andExpect(status().isNotFound());
    }
}
