package com.client.productionreview.service;

import com.client.productionreview.dtos.review.ReviewImageDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.IoFileException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewImage;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewImageRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.impl.ReviewImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewImageServiceTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewImageRepository reviewImageRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private DomainEventPublisher eventPublisher;

    private ReviewImageServiceImpl service;
    private final User author = User.builder().id(2L).name("Autor").build();
    private final User other = User.builder().id(3L).name("Outro").build();
    private User admin;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewImageServiceImpl(reviewRepository, reviewImageRepository, storageService, eventPublisher, "bucket");
        review = Review.builder().id(12L).userId(author.getId()).title("Ótimo").build();
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        admin = User.builder().id(1L).name("Admin").roles(Set.of(adminRole)).build();
    }

    private MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("file", "foto.bin", contentType, content);
    }

    @Test
    void addImage_byAuthor_uploadsWithUuidKeyAndReturnsRelativeUrl() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(12L)).thenReturn(2L);
        when(reviewImageRepository.save(any())).thenAnswer(inv -> {
            ReviewImage image = inv.getArgument(0);
            image.setId(7L);
            return image;
        });

        ReviewImageDTO dto = service.addImage(12L, file("image/jpeg", JPEG), author);

        assertEquals(7L, dto.getId());
        assertTrue(dto.getUrl().matches("/api/v1/files/reviews/12/[0-9a-f-]{36}\\.jpg"), dto.getUrl());
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storageService).uploadFile(eq("bucket"), key.capture(), any(), eq((long) JPEG.length), eq("image/jpeg"));
        assertEquals(dto.getUrl(), "/api/v1/files/" + key.getValue());
        assertTrue(ReviewImageServiceImpl.isAllowedKey(key.getValue()));
        verify(eventPublisher).publish(eq(EventType.REVIEW_IMAGE_ADDED), eq(12L), contains("adicionou uma foto"), eq(author));
    }

    @Test
    void addImage_acceptsPngAndWebp() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.addImage(12L, file("image/png", PNG), author).getUrl().endsWith(".png"));
        assertTrue(service.addImage(12L, file("image/webp", WEBP), author).getUrl().endsWith(".webp"));
    }

    @Test
    void addImage_notAuthor_is403_evenForAdmin() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

        GlobalException e = assertThrows(GlobalException.class, () -> service.addImage(12L, file("image/jpeg", JPEG), other));
        assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        assertThrows(GlobalException.class, () -> service.addImage(12L, file("image/jpeg", JPEG), admin));
        verifyNoInteractions(storageService);
    }

    @Test
    void addImage_missingReview_is404() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.addImage(99L, file("image/jpeg", JPEG), author));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "application/pdf", "text/html"})
    void addImage_unsupportedType_is400(String contentType) {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class, () -> service.addImage(12L, file(contentType, JPEG), author));
        verifyNoInteractions(storageService);
    }

    @Test
    void addImage_contentNotMatchingDeclaredType_is400() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

        // HTML enviado como image/jpeg
        assertThrows(BadRequestException.class,
                () -> service.addImage(12L, file("image/jpeg", "<html><script>".getBytes()), author));
        assertThrows(BadRequestException.class, () -> service.addImage(12L, file("image/png", JPEG), author));
        verifyNoInteractions(storageService);
    }

    @Test
    void addImage_over5MB_is400() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        byte[] big = new byte[(int) ReviewImageServiceImpl.MAX_SIZE_BYTES + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.addImage(12L, file("image/jpeg", big), author));
        assertEquals("A foto deve ter no máximo 5 MB", e.getMessage());
    }

    @Test
    void addImage_fourthImage_is400() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(12L)).thenReturn(3L);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.addImage(12L, file("image/jpeg", JPEG), author));
        assertEquals("Limite de 3 fotos por avaliação", e.getMessage());
        verifyNoInteractions(storageService);
    }

    @Test
    void addImage_emptyFile_is400() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class, () -> service.addImage(12L, file("image/jpeg", new byte[0]), author));
    }

    @Test
    void addImage_storageFailure_doesNotSaveRecord() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(storageService.uploadFile(any(), any(), any(), anyLong(), any())).thenThrow(new IoFileException("falhou"));

        assertThrows(IoFileException.class, () -> service.addImage(12L, file("image/jpeg", JPEG), author));
        verify(reviewImageRepository, never()).save(any());
    }

    @Test
    void deleteImage_byAuthorOrAdmin() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        ReviewImage image = ReviewImage.builder().id(7L).reviewId(12L).objectKey("reviews/12/a.jpg").build();
        when(reviewImageRepository.findById(7L)).thenReturn(Optional.of(image));

        service.deleteImage(12L, 7L, author);
        service.deleteImage(12L, 7L, admin);

        verify(reviewImageRepository, times(2)).delete(image);
        verify(storageService, times(2)).deleteFile("bucket", "reviews/12/a.jpg");
        verify(eventPublisher, times(2)).publish(eq(EventType.REVIEW_IMAGE_REMOVED), eq(12L), anyString(), any());
    }

    @Test
    void deleteImage_otherUser_is403() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

        assertThrows(GlobalException.class, () -> service.deleteImage(12L, 7L, other));
        verify(reviewImageRepository, never()).delete(any());
    }

    @Test
    void deleteImage_ofAnotherReview_is404() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findById(7L))
                .thenReturn(Optional.of(ReviewImage.builder().id(7L).reviewId(99L).objectKey("k").build()));

        assertThrows(NotFoundException.class, () -> service.deleteImage(12L, 7L, author));
    }

    @Test
    void deleteImage_storageFailure_isTolerated() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        ReviewImage image = ReviewImage.builder().id(7L).reviewId(12L).objectKey("reviews/12/a.jpg").build();
        when(reviewImageRepository.findById(7L)).thenReturn(Optional.of(image));
        doThrow(new IoFileException("fora do ar")).when(storageService).deleteFile(any(), any());

        assertDoesNotThrow(() -> service.deleteImage(12L, 7L, author));
        verify(reviewImageRepository).delete(image);
    }

    @Test
    void deleteAllForReview_removesRecordsAndFiles() {
        when(reviewImageRepository.findByReviewIdOrderByIdAsc(12L)).thenReturn(List.of(
                ReviewImage.builder().id(1L).reviewId(12L).objectKey("reviews/12/a.jpg").build(),
                ReviewImage.builder().id(2L).reviewId(12L).objectKey("reviews/12/b.png").build()));

        service.deleteAllForReview(12L);

        verify(reviewImageRepository).deleteByReview(12L);
        verify(storageService).deleteFile("bucket", "reviews/12/a.jpg");
        verify(storageService).deleteFile("bucket", "reviews/12/b.png");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret.txt", "reviews/../../etc/passwd", "reviews/12/../../x.jpg", "/reviews/12/a.jpg",
            "reviews/12/0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.exe", "outro/12/0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.jpg",
            "reviews/12//0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.jpg", "reviews/abc/0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.jpg",
            "reviews\\12\\0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.jpg", "reviews/12/%2e%2e.jpg", ""})
    void openFile_rejectsKeysOutsideAllowedPrefixes(String key) {
        assertThrows(BadRequestException.class, () -> service.openFile(key));
        verifyNoInteractions(storageService);
    }

    @Test
    void openFile_validKey_readsFromStorage() {
        String key = "reviews/12/0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.jpg";
        StorageService.StoredObject object = new StorageService.StoredObject(new ByteArrayInputStream(JPEG), JPEG.length, "image/jpeg");
        when(storageService.getFile("bucket", key)).thenReturn(object);

        assertSame(object, service.openFile(key));
        assertTrue(ReviewImageServiceImpl.isAllowedKey("product/3/0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.png"));
    }

    @Test
    void contentTypeOf_usesExtension() {
        assertEquals("image/jpeg", service.contentTypeOf("reviews/1/a.jpg"));
        assertEquals("image/png", service.contentTypeOf("reviews/1/a.png"));
        assertEquals("image/webp", service.contentTypeOf("reviews/1/a.webp"));
        assertEquals("application/octet-stream", service.contentTypeOf("product/1/a"));
    }
}
