package com.client.productionreview.service;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.model.jpa.Review;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ReviewService {

    public Review saveReview( Review review , String nameUser);

    public Review updateReview(Review review, Long id);

    public void deleteReview(Long id);

    public Review getReview(Long id);

    public List<Review> getReviews();

    public Flux<NotificationDto> getCommentStream();
}
