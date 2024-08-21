package com.client.productionreview.controller.mapper;

import com.client.productionreview.dtos.review.ReviewRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toModel(ReviewRequestDTO reviewDto) {
        Review review = new Review();
        review.setTitle(reviewDto.getTitle());
        review.setDescription(reviewDto.getDescription());
        review.setNote(reviewDto.getNote());
        review.setProductId(reviewDto.getProductId());
        return review;

    }

    public ReviewResponseDTO toDTO(Review review) {
        ReviewResponseDTO reviewResponseDTO = new ReviewResponseDTO();
        reviewResponseDTO.setId(review.getId());
        reviewResponseDTO.setTitle(review.getTitle());
        reviewResponseDTO.setDescription(review.getDescription());
        reviewResponseDTO.setNote(review.getNote());
        reviewResponseDTO.setProductId(review.getProductId());
        return reviewResponseDTO;
    }


}
