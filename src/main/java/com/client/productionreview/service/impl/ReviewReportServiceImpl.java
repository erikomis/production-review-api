package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.review.ReviewReportDTO;
import com.client.productionreview.dtos.review.ReviewReportRequestDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewReport;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewReportRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ReviewReportService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ReviewReportServiceImpl implements ReviewReportService {

    static final String ALREADY_REPORTED = "Você já denunciou esta avaliação";

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final DomainEventPublisher eventPublisher;

    public ReviewReportServiceImpl(ReviewRepository reviewRepository, ReviewReportRepository reviewReportRepository,
                                   DomainEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ReviewReportDTO report(Long reviewId, ReviewReportRequestDTO request, User user) {
        Review review = reviewRepository.findById(reviewId)
                .filter(found -> found.getStatus() != ReviewStatus.HIDDEN)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        if (Objects.equals(review.getUserId(), user.getId())) {
            throw new BadRequestException("Você não pode denunciar a sua própria avaliação");
        }
        if (reviewReportRepository.existsByReviewIdAndUserId(reviewId, user.getId())) {
            throw new BusinessExcepion(ALREADY_REPORTED);
        }

        String details = request.getDetails() == null || request.getDetails().isBlank() ? null : request.getDetails().trim();
        ReviewReport saved;
        try {
            saved = reviewReportRepository.saveAndFlush(ReviewReport.builder()
                    .reviewId(reviewId)
                    .userId(user.getId())
                    .reason(request.getReason())
                    .details(details)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // duas requisições simultâneas do mesmo usuário
            throw new BusinessExcepion(ALREADY_REPORTED);
        }

        eventPublisher.publish(EventType.REVIEW_REPORTED, reviewId, nameOf(user) + " denunciou a avaliação \""
                + titleOf(review) + "\" (" + request.getReason() + ")", user);

        return ReviewReportDTO.builder()
                .id(saved.getId())
                .reason(saved.getReason())
                .details(saved.getDetails())
                .reporterName(nameOf(user))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public List<ReviewReportDTO> listReports(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NotFoundException("Review not found");
        }
        return reviewReportRepository.findViewsByReview(reviewId).stream()
                .map(view -> ReviewReportDTO.builder()
                        .id(view.getId())
                        .reason(view.getReason())
                        .details(view.getDetails())
                        .reporterName(view.getReporterName() == null ? null : view.getReporterName().trim())
                        .createdAt(view.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public void dismissReports(Long reviewId, User admin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        int removed = reviewReportRepository.deleteByReview(reviewId);
        if (removed > 0) {
            eventPublisher.publish(EventType.REVIEW_REPORTS_DISMISSED, reviewId, nameOf(admin) + " descartou " + removed
                    + (removed == 1 ? " denúncia" : " denúncias") + " da avaliação \"" + titleOf(review) + "\"", admin);
        }
    }

    private static String nameOf(User user) {
        return user != null && user.getName() != null ? user.getName().trim() : "Usuário";
    }

    private static String titleOf(Review review) {
        return review.getTitle() == null ? "" : review.getTitle().trim();
    }
}
