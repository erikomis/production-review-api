package com.client.productionreview.service;

import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;


import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SubCategoryRepository subCategorieRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private Path fileStorageLocation;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setSubCategorieId(1L);
        product.setName("Test Product");

    }


    @Test
    void testAddProductSuccess() {
        when(subCategorieRepository.findById(anyLong())).thenReturn(Optional.of(new SubCategory()));
        when(productRepository.save(product)).thenReturn(product);
        Product result = productService.addProduct(product);
        assertNotNull(result);
        assertEquals(product.getName(), result.getName());
        verify(productRepository, times(1)).save(product);
    }




    @Test
    void testAddProductSubCategoryNotFound() {
        when(subCategorieRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            productService.addProduct(product);
        });

        assertEquals("SubCategorie not exists", exception.getMessage());
    }


    @Test
    void testUpdateProductSuccess() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(subCategorieRepository.findById(anyLong())).thenReturn(Optional.of(new SubCategory()));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product updatedProduct = productService.updateProduct(product, 1L);

        assertNotNull(updatedProduct);
        assertEquals(product.getName(), updatedProduct.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }


    @Test
    void testUpdateProduct_updatesExistingRecord() {
        Product incoming = new Product();
        incoming.setName("New name");
        incoming.setSlug("new-slug");
        incoming.setDescription("new description");
        incoming.setSubCategorieId(2L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(subCategorieRepository.findById(2L)).thenReturn(Optional.of(new SubCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = productService.updateProduct(incoming, 1L);

        // antes da correção o produto recebido (sem id) era salvo, criando um novo registro
        assertEquals(1L, updated.getId());
        assertEquals("New name", updated.getName());
        assertEquals(2L, updated.getSubCategorieId());
    }

    @Test
    void testUpdateProductNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            productService.updateProduct(product, 1L);
        });

        assertEquals("Product not found", exception.getMessage());
    }


    @Test
    void testDeleteProductSuccess() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).delete(any(Product.class));
    }


    @Test
    void testDeleteProductNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            productService.deleteProduct(1L);
        });

        assertEquals("Product not found", exception.getMessage());
    }


    @Test
    void testGetProductSuccess() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));

        Product foundProduct = productService.getProduct(1L);

        assertNotNull(foundProduct);
        assertEquals(product.getName(), foundProduct.getName());
    }


    @Test
    void testGetProductBySlug() {
        when(productRepository.findBySlug("smartphone")).thenReturn(Optional.of(product));

        assertEquals(product, productService.getProductBySlug("smartphone"));
    }

    @Test
    void testGetProductBySlugNotFound() {
        when(productRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductBySlug("nope"));
    }

    @Test
    void testGetProductNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            productService.getProduct(1L);
        });

        assertEquals("Product not found", exception.getMessage());
    }


    private ProductSummaryDTO summary() {
        return new ProductSummaryDTO(1L, "Test Product", "d", "test-product", 1L, "Sub", "sub", 2L, "Cat", "cat", null, 4.25, 4L);
    }

    @Test
    void testListProducts_delegatesFilterAndPageable() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "averageNote"));
        ProductFilter filter = new ProductFilter("Test", 2L, null, true);
        when(productRepository.findSummaries(filter, pageable)).thenReturn(new PageImpl<>(List.of(summary())));

        Page<ProductSummaryDTO> result = productService.listProducts(filter, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(4.3, result.getContent().get(0).getAverageNote());
        assertEquals(4L, result.getContent().get(0).getTotalReviews());
    }

    @Test
    void testListProducts_invalidSortProperty_throwsBadRequest() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));

        assertThrows(BadRequestException.class,
                () -> productService.listProducts(new ProductFilter(null, null, null, false), pageable));
        verifyNoInteractions(productRepository);
    }

    @Test
    void testGetProductDetail_includesAllImagesInOrder() {
        when(productRepository.findSummaries(eq(ProductFilter.byId(1L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary())));
        ProductImage first = new ProductImage();
        first.setId(5L);
        first.setUrlImage("https://img/1.jpg");
        ProductImage second = new ProductImage();
        second.setId(6L);
        second.setUrlImage("https://img/2.jpg");
        when(productImageRepository.findByProductIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));

        ProductDetailDTO detail = productService.getProductDetail(1L);

        assertEquals("Test Product", detail.getName());
        assertEquals("https://img/1.jpg", detail.getImageUrl());
        assertEquals(2, detail.getImages().size());
        assertEquals(6L, detail.getImages().get(1).getId());
        assertEquals("Cat", detail.getCategoryName());
    }

    @Test
    void testGetProductDetailBySlug_notFound() {
        when(productRepository.findSummaries(eq(ProductFilter.bySlug("nope")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThrows(NotFoundException.class, () -> productService.getProductDetailBySlug("nope"));
    }

    @Test
    void testAddProduct_publishesEvent() {
        when(subCategorieRepository.findById(anyLong())).thenReturn(Optional.of(new SubCategory()));
        when(productRepository.save(product)).thenReturn(product);

        productService.addProduct(product);

        verify(eventPublisher).publish(eq(EventType.PRODUCT_CREATED), eq(1L), anyString());
    }

    @Test
    void testDeleteProduct_publishesEvent() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
        verify(eventPublisher).publish(eq(EventType.PRODUCT_DELETED), eq(1L), anyString());
    }

    // ---------- autocompletar ----------

    @Test
    void suggest_normalizesTermAndPassesLimit() {
        var item = new com.client.productionreview.dtos.product.ProductSuggestionDTO(1L, "Café", "cafe", "Bebidas");
        when(productRepository.suggest("cafe", 8)).thenReturn(List.of(item));

        assertEquals(List.of(item), productService.suggest("  CAFÉ ", 8));
    }

    @Test
    void suggest_validatesTermAndLimit() {
        assertThrows(BadRequestException.class, () -> productService.suggest("c", 8));
        assertThrows(BadRequestException.class, () -> productService.suggest("  ", 8));
        assertThrows(BadRequestException.class, () -> productService.suggest(null, 8));
        assertThrows(BadRequestException.class, () -> productService.suggest("cafe", 11));
        assertThrows(BadRequestException.class, () -> productService.suggest("cafe", 0));
        verifyNoInteractions(productRepository);
    }
}
