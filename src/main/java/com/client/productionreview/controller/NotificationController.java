package com.client.productionreview.controller;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {


    private final ReviewService reviewService;

    public NotificationController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SecurityRequirement(name = "jwt_auth")
    public Flux<NotificationDto> streamComments() {
        return reviewService.getCommentStream();
    }

}
