package com.client.productionreview.controller;

import com.client.productionreview.dtos.review.ReviewModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ReviewModerationService;
import com.client.productionreview.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@SecurityRequirement(name = "jwt_auth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminReviewController {

    private final ReviewModerationService reviewModerationService;

    public AdminReviewController(ReviewModerationService reviewModerationService) {
        this.reviewModerationService = reviewModerationService;
    }

    @GetMapping
    public Page<ReviewResponseDTO> listReviews(@RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false) Integer size,
                                               @RequestParam(value = "status", required = false) ReviewStatus status,
                                               @RequestParam(value = "note", required = false) Long note,
                                               @RequestParam(value = "productId", required = false) Long productId,
                                               @RequestParam(value = "search", required = false) String search) {
        return reviewModerationService.listReviews(status, note, productId, search,
                PaginationUtils.createPageable(page, size, null, null));
    }

    @PatchMapping("/{id}/moderation")
    public ReviewResponseDTO moderate(@PathVariable("id") Long id, @Valid @RequestBody ReviewModerationRequestDTO request,
                                      @AuthenticationPrincipal User admin) {
        return reviewModerationService.moderate(id, request, admin);
    }
}
