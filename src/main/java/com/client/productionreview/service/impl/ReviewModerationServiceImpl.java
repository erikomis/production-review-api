package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.review.ReviewBulkModerationRequestDTO;
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
import com.client.productionreview.repositories.jpa.ReviewReportRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.NotificationService;
import com.client.productionreview.service.ReviewModerationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ReviewModerationServiceImpl implements ReviewModerationService {

    private final ReviewRepository reviewRepository;

    private final DomainEventPublisher eventPublisher;

    private final ReviewReportRepository reviewReportRepository;

    private final NotificationService notificationService;

    private final ReviewEnricher reviewEnricher;

    public ReviewModerationServiceImpl(ReviewRepository reviewRepository, DomainEventPublisher eventPublisher,
                                       ReviewReportRepository reviewReportRepository,
                                       NotificationService notificationService, ReviewEnricher reviewEnricher) {
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
        this.reviewReportRepository = reviewReportRepository;
        this.notificationService = notificationService;
        this.reviewEnricher = reviewEnricher;
    }

    @Override
    public Page<ReviewResponseDTO> listReviews(ReviewStatus status, Long note, Long productId, String search,
                                               Boolean reported, Pageable pageable) {
        if (note != null && (note < 1 || note > 5)) {
            throw new BadRequestException("A nota deve estar entre 1 e 5");
        }
        Page<ReviewResponseDTO> page = reviewRepository.searchDetails(ReviewSearch.builder()
                .status(status).note(note).productId(productId).search(search)
                .reported(Boolean.TRUE.equals(reported) ? Boolean.TRUE : null)
                .sort(ReviewSort.recent).build(), pageable);
        reviewEnricher.enrich(page.getContent(), true);
        return page;
    }

    // ocultar/restaurar muda a média dos produtos e as listagens públicas
    @Override
    @CacheEvict(value = {"review", "product", "seo"}, allEntries = true)
    public ReviewResponseDTO moderate(Long reviewId, ReviewModerationRequestDTO request, User admin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        String reason = reasonOf(request.getStatus(), request.getReason());
        boolean hide = request.getStatus() == ReviewStatus.HIDDEN;

        apply(review, request.getStatus(), reason, admin);

        String title = review.getTitle() == null ? "" : review.getTitle().trim();
        String adminName = nameOf(admin);
        if (hide) {
            eventPublisher.publish(EventType.REVIEW_HIDDEN, reviewId,
                    adminName + " ocultou a avaliação \"" + title + "\": " + reason, admin);
        } else {
            eventPublisher.publish(EventType.REVIEW_RESTORED, reviewId,
                    adminName + " restaurou a avaliação \"" + title + "\"", admin);
        }

        return details(reviewId);
    }

    @Override
    @CacheEvict(value = {"review", "product", "seo"}, allEntries = true)
    public int moderateBulk(ReviewBulkModerationRequestDTO request, User admin) {
        String reason = reasonOf(request.getStatus(), request.getReason());

        List<Long> ids = List.copyOf(new LinkedHashSet<>(request.getIds()));
        List<Review> reviews = reviewRepository.findAllById(ids);
        reviews.forEach(review -> apply(review, request.getStatus(), reason, admin));

        if (!reviews.isEmpty()) {
            boolean hide = request.getStatus() == ReviewStatus.HIDDEN;
            String idList = reviews.stream().map(Review::getId).sorted().map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            eventPublisher.publish(EventType.REVIEWS_BULK_MODERATED, null, nameOf(admin)
                    + (hide ? " ocultou " : " restaurou ") + reviews.size()
                    + (reviews.size() == 1 ? " avaliação" : " avaliações") + " em lote (" + idList + ")"
                    + (hide ? ": " + reason : ""), admin);
        }
        return reviews.size();
    }

    @Override
    @CacheEvict(value = {"review", "seo"}, allEntries = true)
    public ReviewResponseDTO reply(Long reviewId, String text, User admin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        String reply = text == null ? "" : text.trim();
        if (reply.isEmpty()) {
            throw new BadRequestException("text: O texto da resposta é obrigatório");
        }
        boolean edited = review.getReplyText() != null;

        review.setReplyText(reply);
        review.setReplyAuthorId(admin != null ? admin.getId() : null);
        review.setRepliedAt(Instant.now());
        reviewRepository.save(review);

        String title = review.getTitle() == null ? "" : review.getTitle().trim();
        eventPublisher.publish(EventType.REVIEW_REPLIED, reviewId, nameOf(admin)
                + (edited ? " editou a resposta oficial da avaliação \"" : " respondeu a avaliação \"") + title + "\"", admin);
        if (admin == null || !admin.getId().equals(review.getUserId())) {
            notificationService.reviewReplied(review, reply);
        }
        return details(reviewId);
    }

    @Override
    @CacheEvict(value = {"review", "seo"}, allEntries = true)
    public void deleteReply(Long reviewId, User admin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (review.getReplyText() == null) {
            return;
        }
        review.setReplyText(null);
        review.setReplyAuthorId(null);
        review.setRepliedAt(null);
        reviewRepository.save(review);

        String title = review.getTitle() == null ? "" : review.getTitle().trim();
        eventPublisher.publish(EventType.REVIEW_REPLY_DELETED, reviewId,
                nameOf(admin) + " removeu a resposta oficial da avaliação \"" + title + "\"", admin);
    }

    /** Regras comuns ao individual e ao lote; notifica o autor só quando o status muda. */
    private void apply(Review review, ReviewStatus status, String reason, User admin) {
        boolean changed = review.getStatus() != status;
        boolean hide = status == ReviewStatus.HIDDEN;

        review.setStatus(status);
        review.setModerationReason(reason);
        review.setModeratedAt(Instant.now());
        review.setModeratedBy(admin != null ? admin.getId() : null);
        reviewRepository.save(review);

        if (hide) {
            // ocultar resolve as denúncias pendentes
            reviewReportRepository.deleteByReview(review.getId());
        }
        if (changed) {
            if (hide) {
                notificationService.reviewHidden(review, reason);
            } else {
                notificationService.reviewRestored(review);
            }
        }
    }

    private static String reasonOf(ReviewStatus status, String rawReason) {
        String reason = rawReason == null || rawReason.isBlank() ? null : rawReason.trim();
        if (status == ReviewStatus.HIDDEN && reason == null) {
            throw new BadRequestException("reason: O motivo é obrigatório para ocultar uma avaliação");
        }
        return reason;
    }

    private ReviewResponseDTO details(Long reviewId) {
        ReviewResponseDTO dto = reviewRepository.searchDetails(ReviewSearch.builder().reviewId(reviewId).build(), Pageable.ofSize(1))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Review not found"));
        reviewEnricher.enrich(List.of(dto), true);
        return dto;
    }

    private static String nameOf(User admin) {
        return admin != null && admin.getName() != null ? admin.getName().trim() : "Administrador";
    }
}
