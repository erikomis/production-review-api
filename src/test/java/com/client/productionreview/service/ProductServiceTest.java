package com.client.productionreview.service;

import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Product;
import com.client.productionreview.model.SubCategory;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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
    void testGetProductNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            productService.getProduct(1L);
        });

        assertEquals("Product not found", exception.getMessage());
    }


    @Test
    void testGetAllProductWithoutSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(product));

        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Product> result = productService.getAllProduct(pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetAllProductWithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(product));

        when(productRepository.findAllByProduct(anyString(), any(Pageable.class))).thenReturn(page);

        Page<Product> result = productService.getAllProduct(pageable, "Test");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
