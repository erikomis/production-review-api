package com.client.productionreview.service;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ReviewService {

     Review saveReview( Review review , String nameUser);

     Review updateReview(Review review, Long id, User currentUser);

     void deleteReview(Long id, User currentUser);

     Review getReview(Long id);

     List<Review> getReviews();

     Page<Review> getReviewsByProduct(Long productId, Pageable pageable);

     ReviewSummaryDTO getProductSummary(Long productId);

     Flux<NotificationDto> getCommentStream();
}
