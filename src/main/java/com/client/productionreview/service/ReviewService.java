package com.client.productionreview.service;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.model.jpa.Review;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ReviewService {

     Review saveReview( Review review , String nameUser);

     Review updateReview(Review review, Long id);
     void deleteReview(Long id);

     Review getReview(Long id);

     List<Review> getReviews();

     Flux<NotificationDto> getCommentStream();
}
