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

    // dependência nova do controller (seguir produto); no detalhe só repassa o DTO
    @MockBean
    private com.client.productionreview.service.ProductFollowService productFollowService;

    @org.junit.jupiter.api.BeforeEach
    void passThroughFollowInfo() {
        org.mockito.Mockito.when(productFollowService.withFollowInfo(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

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
        ProductSummaryDTO summary = new ProductSummaryDTO(1L, "Phone", "d", "phone", 2L, "Celulares", "celulares", 3L, "Eletrônicos", "eletronicos",
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
                .andExpect(jsonPath("$.categorySlug").value("eletronicos"))
                .andExpect(jsonPath("$.subCategorieSlug").value("celulares"))
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

    // ---------- fase 3: autocompletar, seguir e campos do detalhe ----------

    @Test
    void suggest_returnsList() throws Exception {
        when(productService.suggest("cafe", 5)).thenReturn(List.of(
                new com.client.productionreview.dtos.product.ProductSuggestionDTO(1L, "Café Pilão", "cafe-pilao",
                        "https://img/1.jpg", "Bebidas")));

        mockMvc.perform(get("/api/v1/production/suggest").param("q", "cafe").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Café Pilão"))
                .andExpect(jsonPath("$[0].slug").value("cafe-pilao"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://img/1.jpg"))
                .andExpect(jsonPath("$[0].categoryName").value("Bebidas"));
    }

    @Test
    void suggest_defaultLimitIs8_andShortTermIs400() throws Exception {
        when(productService.suggest("ca", 8)).thenReturn(List.of());
        when(productService.suggest("c", 8))
                .thenThrow(new com.client.productionreview.exception.BadRequestException("q: informe pelo menos 2 caracteres"));

        mockMvc.perform(get("/api/v1/production/suggest").param("q", "ca")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/production/suggest").param("q", "c"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("q: informe pelo menos 2 caracteres"));
    }

    @Test
    void follow_andUnfollow() throws Exception {
        when(productFollowService.follow(eq(3L), any())).thenReturn(new com.client.productionreview.dtos.product.FollowResponseDTO(true, 4));
        when(productFollowService.unfollow(eq(3L), any())).thenReturn(new com.client.productionreview.dtos.product.FollowResponseDTO(false, 3));

        mockMvc.perform(post("/api/v1/production/{id}/follow", 3L))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"following\":true,\"followersCount\":4}"));
        mockMvc.perform(delete("/api/v1/production/{id}/follow", 3L))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"following\":false,\"followersCount\":3}"));
    }

    @Test
    void detail_includesFollowInfo() throws Exception {
        com.client.productionreview.dtos.product.ProductDetailDTO detail = new com.client.productionreview.dtos.product.ProductDetailDTO();
        detail.setId(3L);
        detail.setCreatedAt(java.time.Instant.parse("2026-10-01T23:14:44Z"));
        when(productService.getProductDetail(3L)).thenReturn(detail);
        when(productFollowService.withFollowInfo(detail)).thenAnswer(inv -> {
            detail.setFollowersCount(4);
            detail.setFollowedByMe(true);
            return detail;
        });

        mockMvc.perform(get("/api/v1/production/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followersCount").value(4))
                .andExpect(jsonPath("$.followedByMe").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-10-01T23:14:44Z"));
    }
}
