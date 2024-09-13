package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ReviewMapper;
import com.client.productionreview.dtos.review.ReviewRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    private final ReviewMapper reviewMapper;

    ReviewController(ReviewService reviewService, ReviewMapper reviewMapper) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
    }


    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponseDTO createReview(@Valid @RequestBody ReviewRequestDTO requestDTO ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        return reviewMapper.toDTO(reviewService.saveReview(reviewMapper.toModel(requestDTO), user.getName()));

    }

    @PutMapping("/{id}")
    public ReviewResponseDTO updateReview(@Valid @RequestBody ReviewRequestDTO requestDTO, @PathVariable("id") Long id) {
        return reviewMapper.toDTO(reviewService.updateReview(reviewMapper.toModel(requestDTO), id));
    }



    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable("id") Long id) {
        reviewService.deleteReview(id);
    }

    @GetMapping("/{id}")
    public ReviewResponseDTO getReview(@PathVariable("id") Long id) {
        return reviewMapper.toDTO(reviewService.getReview(id));
    }


 @GetMapping("/list")
    public List<Review> getReviews() {
        return reviewService.getReviews();
    }




}
