package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.review.ReviewImageDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.repositories.jpa.ReviewImageRepository;
import com.client.productionreview.repositories.jpa.ReviewReportRepository;
import com.client.productionreview.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Completa as reviews de uma página com fotos, "denunciada por mim" e (na moderação) total de denúncias. */
@Component
public class ReviewEnricher {

    private final ReviewImageRepository reviewImageRepository;

    private final ReviewReportRepository reviewReportRepository;

    public ReviewEnricher(ReviewImageRepository reviewImageRepository, ReviewReportRepository reviewReportRepository) {
        this.reviewImageRepository = reviewImageRepository;
        this.reviewReportRepository = reviewReportRepository;
    }

    public void enrich(List<ReviewResponseDTO> reviews, boolean withReportsCount) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        List<Long> ids = reviews.stream().map(ReviewResponseDTO::getId).toList();

        Map<Long, List<ReviewImageDTO>> images = new HashMap<>();
        reviewImageRepository.findByReviewIdInOrderByIdAsc(ids).forEach(image -> images
                .computeIfAbsent(image.getReviewId(), id -> new ArrayList<>())
                .add(ReviewImageDTO.of(image.getId(), image.getObjectKey())));
        reviews.forEach(review -> review.setImages(images.getOrDefault(review.getId(), new ArrayList<>())));

        Long userId = CurrentUser.id();
        if (userId != null) {
            Set<Long> reported = new HashSet<>(reviewReportRepository.findReviewIdsReportedBy(userId, ids));
            reviews.forEach(review -> review.setReportedByMe(reported.contains(review.getId())));
        }

        if (withReportsCount) {
            Map<Long, Long> counts = new HashMap<>();
            reviewReportRepository.countByReviews(ids).forEach(count -> counts.put(count.getReviewId(), count.getTotal()));
            reviews.forEach(review -> review.setReportsCount(counts.getOrDefault(review.getId(), 0L)));
        }
    }
}
