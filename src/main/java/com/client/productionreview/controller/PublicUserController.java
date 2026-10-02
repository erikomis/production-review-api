package com.client.productionreview.controller;

import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.user.PublicProfileDTO;
import com.client.productionreview.service.ReviewService;
import com.client.productionreview.service.UserService;
import com.client.productionreview.utils.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/** Perfil público dos usuários (rotas GET públicas). */
@RestController
@RequestMapping("/api/v1/users")
public class PublicUserController {

    private final UserService userService;

    private final ReviewService reviewService;

    public PublicUserController(UserService userService, ReviewService reviewService) {
        this.userService = userService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{username}")
    public PublicProfileDTO profile(@PathVariable("username") String username) {
        return userService.getPublicProfile(username);
    }

    @GetMapping("/{username}/reviews")
    public Page<ReviewResponseDTO> reviews(@PathVariable("username") String username,
                                           @RequestParam(value = "page", required = false) Integer page,
                                           @RequestParam(value = "size", required = false) Integer size) {
        return reviewService.getUserReviews(username, PaginationUtils.createPageable(page, size, null, null));
    }
}
