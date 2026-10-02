package com.client.productionreview.service;


import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;

import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.impl.ReviewServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository  reviewRepository;

    @Mock
    private ProductionReviewApiProducer productionReviewApiProducer ;

    private Review review;
    private Product product;
    private User owner;
    private User otherUser;
    private User admin;
    private Long reviewId;
    private Long productId;

    @BeforeEach
    public void setUp() {
        reviewId = 1L;
        productId = 2L;
        product = new Product();
        product.setId(productId);
        product.setName("product");
        review = new Review();
        review.setProductId(productId);
        review.setTitle("title");
        review.setDescription("description");
        review.setNote(5L);

        owner = User.builder().id(10L).name("owner").build();
        otherUser = User.builder().id(20L).name("other").build();
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        admin = User.builder().id(30L).name("admin").roles(List.of(adminRole)).build();
    }

    private Review existingReview() {
        Review existing = new Review();
        existing.setId(reviewId);
        existing.setUserId(owner.getId());
        existing.setProductId(productId);
        existing.setTitle("old title");
        return existing;
    }


    @Test
    public void testSaveReview_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.saveReview(review, "nameUser"));
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(productionReviewApiProducer);
    }

    @Test
    public void testSaveReview_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenReturn(review);

        Review savedReview = reviewService.saveReview(review, "nameUser");

        assertNotNull(savedReview);
        verify(reviewRepository).save(review);

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(productionReviewApiProducer).sendNotification(captor.capture());
        assertEquals("nameUser", captor.getValue().getNameUser());
    }

    @Test
    public void testSaveReview_KafkaFailureDoesNotBreakCreation() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenReturn(review);
        doThrow(new RuntimeException("broker down")).when(productionReviewApiProducer).sendNotification(any());

        assertNotNull(reviewService.saveReview(review, "nameUser"));
    }

    @Test
    public void testCommentStream_SurvivesSubscriberCancel() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any())).thenReturn(review);

        // primeiro assinante entra e sai
        StepVerifier.create(reviewService.getCommentStream())
                .then(() -> reviewService.saveReview(review, "first"))
                .assertNext(n -> assertEquals("first", n.getNameUser()))
                .thenCancel()
                .verify();

        // o stream deve continuar funcionando para novos assinantes
        StepVerifier.create(reviewService.getCommentStream())
                .then(() -> reviewService.saveReview(review, "second"))
                .assertNext(n -> assertEquals("second", n.getNameUser()))
                .thenCancel()
                .verify();
    }

    @Test
    public void testUpdateReview_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.updateReview(review, reviewId, owner));
    }

    @Test
    public void testUpdateReview_ReviewNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.updateReview(review, reviewId, owner));
    }

    @Test
    public void testUpdateReview_UpdatesExistingRecordAndKeepsOwner() {
        Review existing = existingReview();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // o mapper monta a review sem id; o service deve atualizar a existente
        Review updatedReview = reviewService.updateReview(review, reviewId, owner);

        assertEquals(reviewId, updatedReview.getId());
        assertEquals(owner.getId(), updatedReview.getUserId());
        assertEquals("title", updatedReview.getTitle());
        assertEquals(5L, updatedReview.getNote());
    }

    @Test
    public void testUpdateReview_ForbiddenForOtherUser() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        GlobalException ex = assertThrows(GlobalException.class, () -> reviewService.updateReview(review, reviewId, otherUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    public void testUpdateReview_AllowedForAdmin() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review updated = reviewService.updateReview(review, reviewId, admin);

        assertEquals(owner.getId(), updated.getUserId());
    }

    @Test
    public void testDeleteReview_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.deleteReview(reviewId, owner));
    }

    @Test
    public void testDeleteReview_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        reviewService.deleteReview(reviewId, owner);

        verify(reviewRepository).deleteById(reviewId);
    }

    @Test
    public void testDeleteReview_ForbiddenForOtherUser() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        assertThrows(GlobalException.class, () -> reviewService.deleteReview(reviewId, otherUser));

        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    public void testGetReview_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.getReview(reviewId));
    }

    @Test
    public void testGetReview_Success() {

        review.setId(1L);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        Review fetchedReview = reviewService.getReview(reviewId);

        assertNotNull(fetchedReview);
        assertEquals(reviewId, fetchedReview.getId());
    }

    @Test
    public void testGetReviews() {
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        List<Review> reviews = reviewService.getReviews();

        assertNotNull(reviews);
        assertFalse(reviews.isEmpty());
        assertEquals(1, reviews.size());
    }

    @Test
    public void testGetReviewsByProduct() {
        var pageable = PageRequest.of(0, 10);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProductId(productId, pageable)).thenReturn(new PageImpl<>(List.of(review)));

        Page<Review> page = reviewService.getReviewsByProduct(productId, pageable);

        assertEquals(1, page.getTotalElements());
    }

    @Test
    public void testGetReviewsByProduct_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.getReviewsByProduct(productId, PageRequest.of(0, 10)));
    }

    @Test
    public void testGetProductSummary_RoundsAverage() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.getRatingSummary(productId)).thenReturn(summary(3L, 4.3333));

        ReviewSummaryDTO summary = reviewService.getProductSummary(productId);

        assertEquals(productId, summary.getProductId());
        assertEquals(3L, summary.getTotalReviews());
        assertEquals(4.3, summary.getAverageNote());
    }

    @Test
    public void testGetProductSummary_NoReviews() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.getRatingSummary(productId)).thenReturn(summary(0L, null));

        ReviewSummaryDTO summary = reviewService.getProductSummary(productId);

        assertEquals(0L, summary.getTotalReviews());
        assertEquals(0.0, summary.getAverageNote());
    }

    private ReviewRepository.RatingSummary summary(Long total, Double average) {
        return new ReviewRepository.RatingSummary() {
            @Override
            public Long getTotalReviews() {
                return total;
            }

            @Override
            public Double getAverageNote() {
                return average;
            }
        };
    }

}
