package com.client.productionreview.controller;


import com.client.productionreview.controller.mapper.ProductMapper;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ProductController.class)
@Import(ProductMapper.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @SpyBean
    private ProductMapper productMapper;

    private Product product() {
        ProductImage image = new ProductImage();
        image.setUrlImage("https://storage/img.png");
        return Product.builder().id(1L).name("Phone").description("d").slug("phone")
                .subCategorieId(2L).productImages(List.of(image)).build();
    }

    @Test
    void addProduct_shouldReturnCreated() throws Exception {
        when(productService.addProduct(any(Product.class))).thenReturn(product());

        mockMvc.perform(post("/api/v1/production/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"description\":\"d\",\"slug\":\"phone\",\"subCategorieId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.imageUrl").value("https://storage/img.png"));
    }

    @Test
    void addProduct_invalidBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/production/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void getProduct_shouldReturnProduct() throws Exception {
        when(productService.getProduct(1L)).thenReturn(product());

        mockMvc.perform(get("/api/v1/production/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.subCategorieId").value(2));
    }

    @Test
    void getProduct_notFound_shouldReturn404() throws Exception {
        when(productService.getProduct(9L)).thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/production/{id}", 9L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listProducts_passesPaginationAndSearch() throws Exception {
        when(productService.getAllProduct(any(Pageable.class), eq("pho"))).thenReturn(new PageImpl<>(List.of(product())));

        mockMvc.perform(get("/api/v1/production/list")
                        .param("page", "1").param("size", "5")
                        .param("sort", "DESC").param("property", "name")
                        .param("search", "pho"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Phone"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProduct(captor.capture(), eq("pho"));
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    void listProducts_invalidSortDirection_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/production/list").param("sort", "SIDEWAYS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_shouldReturnUpdated() throws Exception {
        when(productService.updateProduct(any(Product.class), eq(1L))).thenReturn(product());

        mockMvc.perform(put("/api/v1/production/update/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"description\":\"d\",\"slug\":\"phone\",\"subCategorieId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteProduct_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/production/delete/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }
}
