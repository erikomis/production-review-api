package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ReviewMapper;
import com.client.productionreview.dtos.review.ReviewRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ReviewService;
import com.client.productionreview.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @SecurityRequirement(name = "jwt_auth")
    public ReviewResponseDTO createReview(@Valid @RequestBody ReviewRequestDTO requestDTO, @AuthenticationPrincipal User user) {
        return reviewMapper.toDTO(reviewService.saveReview(reviewMapper.toModel(requestDTO, user.getId()), user.getName()));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "jwt_auth")
    public ReviewResponseDTO updateReview(@Valid @RequestBody ReviewRequestDTO requestDTO, @PathVariable("id") Long id,
                                          @AuthenticationPrincipal User user) {
        return reviewMapper.toDTO(reviewService.updateReview(reviewMapper.toModel(requestDTO, user.getId()), id, user));
    }



    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt_auth")
    public void deleteReview(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        reviewService.deleteReview(id, user);
    }

    @GetMapping("/{id}")
    public ReviewResponseDTO getReview(@PathVariable("id") Long id) {
        return reviewMapper.toDTO(reviewService.getReview(id));
    }


    @GetMapping("/list")
    public List<ReviewResponseDTO> getReviews() {
        return reviewService.getReviews().stream().map(reviewMapper::toDTO).toList();
    }

    @GetMapping("/product/{productId}")
    public Page<ReviewResponseDTO> getReviewsByProduct(@PathVariable("productId") Long productId,
                                                       @RequestParam(value = "page", required = false) Integer page,
                                                       @RequestParam(value = "size", required = false) Integer size,
                                                       @RequestParam(value = "sort", required = false) Sort.Direction sort) {
        var pageable = PaginationUtils.createPageable(page, size, "createdAt", sort != null ? sort.name() : Sort.Direction.DESC.name());
        return reviewService.getReviewsByProduct(productId, pageable).map(reviewMapper::toDTO);
    }

    @GetMapping("/product/{productId}/summary")
    public ReviewSummaryDTO getProductSummary(@PathVariable("productId") Long productId) {
        return reviewService.getProductSummary(productId);
    }

}
