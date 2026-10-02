package com.client.productionreview.service;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.dtos.review.HelpfulResponseDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;


public interface ReviewService {

     Review saveReview( Review review , String nameUser);

     Review updateReview(Review review, Long id, User currentUser);

     void deleteReview(Long id, User currentUser);

     Review getReview(Long id);

     /** Review com nomes e "útil"; ocultas só aparecem para o autor ou ADMIN (senão 404). */
     ReviewResponseDTO getReviewDetails(Long id);

     /** Reviews visíveis; a direção de createdAt do pageable define recent/oldest. */
     Page<ReviewResponseDTO> getReviews(Pageable pageable);

     /** Reviews visíveis do produto. {@code note} 1..5 opcional; {@code sort} recent|oldest|highest|lowest|helpful. */
     Page<ReviewResponseDTO> getReviewsByProduct(Long productId, Long note, String sort, Pageable pageable);

     /** Reviews do usuário, inclusive as ocultadas. */
     Page<ReviewResponseDTO> getMyReviews(Long userId, Pageable pageable);

     ReviewSummaryDTO getProductSummary(Long productId);

     /** Marca/desmarca a review como útil para o usuário. */
     HelpfulResponseDTO toggleHelpful(Long reviewId, User user);

     Flux<NotificationDto> getCommentStream();
}
