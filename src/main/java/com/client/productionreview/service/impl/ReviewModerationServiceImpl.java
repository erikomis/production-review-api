package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.review.ReviewModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ReviewModerationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReviewModerationServiceImpl implements ReviewModerationService {

    private final ReviewRepository reviewRepository;

    private final DomainEventPublisher eventPublisher;

    public ReviewModerationServiceImpl(ReviewRepository reviewRepository, DomainEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Page<ReviewResponseDTO> listReviews(ReviewStatus status, Long note, Long productId, String search, Pageable pageable) {
        if (note != null && (note < 1 || note > 5)) {
            throw new BadRequestException("A nota deve estar entre 1 e 5");
        }
        return reviewRepository.searchDetails(ReviewSearch.builder()
                .status(status).note(note).productId(productId).search(search).sort(ReviewSort.recent).build(), pageable);
    }

    // ocultar/restaurar muda a média dos produtos e as listagens públicas
    @Override
    @CacheEvict(value = {"review", "product"}, allEntries = true)
    public ReviewResponseDTO moderate(Long reviewId, ReviewModerationRequestDTO request, User admin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        String reason = request.getReason() == null || request.getReason().isBlank() ? null : request.getReason().trim();
        boolean hide = request.getStatus() == ReviewStatus.HIDDEN;

        if (hide && reason == null) {
            throw new BadRequestException("reason: O motivo é obrigatório para ocultar uma avaliação");
        }

        review.setStatus(request.getStatus());
        review.setModerationReason(reason);
        review.setModeratedAt(Instant.now());
        review.setModeratedBy(admin != null ? admin.getId() : null);
        reviewRepository.save(review);

        String title = review.getTitle() == null ? "" : review.getTitle().trim();
        String adminName = admin != null && admin.getName() != null ? admin.getName().trim() : "Administrador";
        if (hide) {
            eventPublisher.publish(EventType.REVIEW_HIDDEN, reviewId,
                    adminName + " ocultou a avaliação \"" + title + "\": " + reason, admin);
        } else {
            eventPublisher.publish(EventType.REVIEW_RESTORED, reviewId,
                    adminName + " restaurou a avaliação \"" + title + "\"", admin);
        }

        return reviewRepository.searchDetails(ReviewSearch.builder().reviewId(reviewId).build(), Pageable.ofSize(1))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Review not found"));
    }
}
