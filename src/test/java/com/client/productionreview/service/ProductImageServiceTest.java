package com.client.productionreview.service;

import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.impl.ProductImageServiceImpl;
import io.minio.ObjectWriteResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DomainEventPublisher eventPublisher;

    private ProductImageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductImageServiceImpl(productImageRepository, productRepository, storageService,
                "https://storage.test", "bucket", eventPublisher);
    }

    private MockMultipartFile image(String name, String contentType) {
        return new MockMultipartFile("file", name, contentType, new byte[]{1, 2, 3});
    }

    @Test
    void upload_storesWithUniqueKeyTypeAndUrl() {
        when(productRepository.existsById(5L)).thenReturn(true);
        when(storageService.uploadFile(eq("bucket"), anyString(), any(InputStream.class), eq(3L), eq("image/png")))
                .thenAnswer(inv -> new ObjectWriteResponse(Headers.of(), "bucket", null, inv.getArgument(1), null, null));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductImage saved = service.createProductImage(image("foto.PNG", "image/png"), 5L);

        assertTrue(saved.getFilename().matches("product/5/[0-9a-f-]{36}\\.png"), saved.getFilename());
        assertEquals("image/png", saved.getType());
        assertEquals("https://storage.test/bucket/" + saved.getFilename(), saved.getUrlImage());
        assertEquals(5L, saved.getProductId());
    }

    @Test
    void upload_sameFilenameTwice_generatesDifferentKeys() {
        when(productRepository.existsById(anyLong())).thenReturn(true);
        when(storageService.uploadFile(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(inv -> new ObjectWriteResponse(Headers.of(), "bucket", null, inv.getArgument(1), null, null));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        var first = service.createProductImage(image("foto.jpg", "image/jpeg"), 1L);
        var second = service.createProductImage(image("foto.jpg", "image/jpeg"), 2L);

        assertNotEquals(first.getFilename(), second.getFilename());
    }

    @Test
    void upload_rejectsNonImage() {
        assertThrows(BadRequestException.class,
                () -> service.createProductImage(image("script.html", "text/html"), 1L));
        verifyNoInteractions(storageService);
    }

    @Test
    void upload_rejectsEmptyFile() {
        var empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThrows(BadRequestException.class, () -> service.createProductImage(empty, 1L));
    }

    @Test
    void upload_productNotFound() {
        when(productRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.createProductImage(image("a.png", "image/png"), 1L));
        verifyNoInteractions(storageService);
    }

    @Test
    void upload_ignoresPathInExtension() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(storageService.uploadFile(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(inv -> new ObjectWriteResponse(Headers.of(), "bucket", null, inv.getArgument(1), null, null));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.createProductImage(image("a./../../etc", "image/png"), 1L);

        assertFalse(saved.getFilename().contains(".."));
    }

    @Test
    void delete_removesFromStorageAndDatabase() {
        var productImage = new ProductImage();
        productImage.setId(3L);
        productImage.setFilename("product/1/key.png");
        when(productImageRepository.findById(3L)).thenReturn(Optional.of(productImage));

        service.deleteFile(3L);

        verify(storageService).deleteFile("bucket", "product/1/key.png");
        verify(productImageRepository).deleteById(3L);
        verify(eventPublisher).publish(eq(EventType.PRODUCT_IMAGE_REMOVED), eq(3L), anyString());
    }

    @Test
    void delete_externalImage_doesNotCallStorage() {
        var productImage = new ProductImage();
        productImage.setId(4L);
        productImage.setFilename("external:off:7891000102626");
        when(productImageRepository.findById(4L)).thenReturn(Optional.of(productImage));

        service.deleteFile(4L);

        // imagem do Open Food Facts não está no MinIO: só o registro é removido
        verifyNoInteractions(storageService);
        verify(productImageRepository).deleteById(4L);
    }

    @Test
    void upload_publishesImageAddedEvent() {
        when(productRepository.existsById(5L)).thenReturn(true);
        when(storageService.uploadFile(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(inv -> new ObjectWriteResponse(Headers.of(), "bucket", null, inv.getArgument(1), null, null));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage saved = inv.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        service.createProductImage(image("foto.png", "image/png"), 5L);

        verify(eventPublisher).publish(eq(EventType.PRODUCT_IMAGE_ADDED), eq(9L), anyString());
    }

    @Test
    void delete_notFound() {
        when(productImageRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteFile(3L));
        verifyNoInteractions(storageService);
    }
}
