package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ReviewMapper;
import com.client.productionreview.dtos.review.HelpfulResponseDTO;
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
        return reviewService.getReviewDetails(id);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "jwt_auth")
    public Page<ReviewResponseDTO> getMyReviews(@RequestParam(value = "page", required = false) Integer page,
                                                @RequestParam(value = "size", required = false) Integer size,
                                                @AuthenticationPrincipal User user) {
        var pageable = PaginationUtils.createPageable(page, size, null, null);
        return reviewService.getMyReviews(user.getId(), pageable);
    }

    @PostMapping("/{id}/helpful")
    @SecurityRequirement(name = "jwt_auth")
    public HelpfulResponseDTO toggleHelpful(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        return reviewService.toggleHelpful(id, user);
    }


    @GetMapping("/list")
    public Page<ReviewResponseDTO> getReviews(@RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "size", required = false) Integer size,
                                              @RequestParam(value = "sort", required = false) Sort.Direction sort) {
        var pageable = PaginationUtils.createPageable(page, size, "createdAt", sort != null ? sort.name() : Sort.Direction.DESC.name());
        return reviewService.getReviews(pageable);
    }

    @GetMapping("/product/{productId}")
    public Page<ReviewResponseDTO> getReviewsByProduct(@PathVariable("productId") Long productId,
                                                       @RequestParam(value = "page", required = false) Integer page,
                                                       @RequestParam(value = "size", required = false) Integer size,
                                                       @RequestParam(value = "note", required = false) Long note,
                                                       @RequestParam(value = "sort", required = false) String sort) {
        var pageable = PaginationUtils.createPageable(page, size, null, null);
        return reviewService.getReviewsByProduct(productId, note, sort, pageable);
    }

    @GetMapping("/product/{productId}/summary")
    public ReviewSummaryDTO getProductSummary(@PathVariable("productId") Long productId) {
        return reviewService.getProductSummary(productId);
    }

}
