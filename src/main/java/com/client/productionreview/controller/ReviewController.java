package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ReviewMapper;
import com.client.productionreview.dtos.review.ReviewRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.Review;
import com.client.productionreview.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/review")
public class ReviewController {

    private final ReviewService reviewService;

    private final ReviewMapper reviewMapper;

    ReviewController(ReviewService reviewService, ReviewMapper reviewMapper) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
    }


    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponseDTO createReview(@Valid @RequestBody ReviewRequestDTO requestDTO) {
        return reviewMapper.toDTO(reviewService.saveReview(reviewMapper.toModel(requestDTO)));

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


 @GetMapping("/")
    public List<Review> getReviews() {
        return reviewService.getReviews();
    }




}
