package com.client.productionreview.controller;


import com.client.productionreview.controller.mapper.ProductMapper;
import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductImageSummaryDTO;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private ProductDetailDTO detail() {
        ProductSummaryDTO summary = new ProductSummaryDTO(1L, "Phone", "d", "phone", 2L, "Celulares", 3L, "Eletrônicos",
                null, 4.333, 3L);
        return ProductDetailDTO.from(summary, List.of(new ProductImageSummaryDTO(5L, "https://storage/img.png"),
                new ProductImageSummaryDTO(6L, "https://storage/img2.png")));
    }

    @Test
    void getProduct_shouldReturnDetailWithRatingAndImages() throws Exception {
        when(productService.getProductDetail(1L)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/production/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.subCategorieId").value(2))
                .andExpect(jsonPath("$.subCategorieName").value("Celulares"))
                .andExpect(jsonPath("$.categoryId").value(3))
                .andExpect(jsonPath("$.categoryName").value("Eletrônicos"))
                .andExpect(jsonPath("$.averageNote").value(4.3))
                .andExpect(jsonPath("$.totalReviews").value(3))
                .andExpect(jsonPath("$.imageUrl").value("https://storage/img.png"))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.images[0].id").value(5))
                .andExpect(jsonPath("$.images[1].urlImage").value("https://storage/img2.png"));
    }

    @Test
    void getProduct_notFound_shouldReturn404() throws Exception {
        when(productService.getProductDetail(9L)).thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/production/{id}", 9L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductBySlug_shouldReturnDetail() throws Exception {
        when(productService.getProductDetailBySlug("phone")).thenReturn(detail());

        mockMvc.perform(get("/api/v1/production/slug/{slug}", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.slug").value("phone"))
                .andExpect(jsonPath("$.images", hasSize(2)));
    }

    @Test
    void listProducts_passesPaginationSearchAndFilters() throws Exception {
        when(productService.listProducts(any(ProductFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(detail())));

        mockMvc.perform(get("/api/v1/production/list")
                        .param("page", "1").param("size", "5")
                        .param("sort", "DESC").param("property", "name")
                        .param("search", "pho").param("categoryId", "3").param("subCategorieId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Phone"))
                .andExpect(jsonPath("$.content[0].averageNote").value(4.3))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
        verify(productService).listProducts(filter.capture(), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("name").getDirection());
        assertEquals(new ProductFilter("pho", 3L, 2L, false), filter.getValue());
    }

    @Test
    void listProducts_ranking_sortsByAverageNoteOnlyRated() throws Exception {
        when(productService.listProducts(any(ProductFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/production/list")
                        .param("property", "averageNote").param("sort", "DESC").param("onlyRated", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
        verify(productService).listProducts(filter.capture(), pageable.capture());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("averageNote").getDirection());
        assertTrue(filter.getValue().onlyRated());
    }

    @Test
    void listProducts_invalidProperty_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/production/list").param("property", "price"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Propriedade de ordenação inválida: price"));

        verifyNoInteractions(productService);
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
