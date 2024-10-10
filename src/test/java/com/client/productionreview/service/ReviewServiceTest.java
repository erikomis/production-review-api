package com.client.productionreview.service;


import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.exception.NotFoundException;

import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.impl.ReviewServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
    private Long reviewId;
    private Long productId;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewId = 1L;
        productId = 2L;
        product = new Product();
        product.setId(productId);
        review = new Review();
        review.setProductId(productId);
    }


    @Test
    public void testSaveReview_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.saveReview(review, "nameUser"));
    }

    @Test
    public void testSaveReview_Success() {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setNameUser("nameUser");
        notificationDto.setAction("criacao de comentario product");
        notificationDto.setMessage("Comentario criado com sucesso");

        product.setName("product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));


        when(reviewRepository.save(review)).thenReturn(review);

        Review savedReview = reviewService.saveReview(review, "nameUser");

        assertNotNull(savedReview);
        verify(reviewRepository).save(review);
    }

    @Test
    public void testUpdateReview_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.updateReview(review, reviewId));
    }

    @Test
    public void testUpdateReview_ReviewNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.updateReview(review, reviewId));
    }

    @Test
    public void testUpdateReview_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        Review updatedReview = reviewService.updateReview(review, reviewId);

        assertNotNull(updatedReview);
        verify(reviewRepository).save(review);
    }

    @Test
    public void testDeleteReview_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.deleteReview(reviewId));
    }

    @Test
    public void testDeleteReview_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId);

        verify(reviewRepository).deleteById(reviewId);
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



}



