package com.client.productionreview.service;

import com.client.productionreview.dtos.review.ReviewReportDTO;
import com.client.productionreview.dtos.review.ReviewReportRequestDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.ReportReason;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewReport;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewReportRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.impl.ReviewReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ReviewReportServiceImpl service;

    private final User reporter = User.builder().id(3L).name("Leitor ").build();
    private final User admin = User.builder().id(1L).name("Admin").build();
    private Review review;

    @BeforeEach
    void setUp() {
        review = Review.builder().id(12L).userId(2L).title("Ótimo").status(ReviewStatus.VISIBLE).build();
    }

    @Test
    void report_savesTrimmedDetailsAndPublishesEvent() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.saveAndFlush(any())).thenAnswer(inv -> {
            ReviewReport report = inv.getArgument(0);
            report.setId(5L);
            report.setCreatedAt(Instant.parse("2026-10-02T10:00:00Z"));
            return report;
        });

        ReviewReportDTO dto = service.report(12L, new ReviewReportRequestDTO(ReportReason.SPAM, "  link suspeito  "), reporter);

        assertEquals(5L, dto.getId());
        assertEquals(ReportReason.SPAM, dto.getReason());
        assertEquals("link suspeito", dto.getDetails());
        assertEquals("Leitor", dto.getReporterName());
        ArgumentCaptor<ReviewReport> saved = ArgumentCaptor.forClass(ReviewReport.class);
        verify(reviewReportRepository).saveAndFlush(saved.capture());
        assertEquals(3L, saved.getValue().getUserId());
        verify(eventPublisher).publish(eq(EventType.REVIEW_REPORTED), eq(12L), contains("(SPAM)"), eq(reporter));
    }

    @Test
    void report_blankDetailsBecomeNull() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertNull(service.report(12L, new ReviewReportRequestDTO(ReportReason.OTHER, "   "), reporter).getDetails());
    }

    @Test
    void report_ownReview_is400() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> service.report(12L, new ReviewReportRequestDTO(ReportReason.SPAM, null), User.builder().id(2L).build()));
        verify(reviewReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void report_twice_is409() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.existsByReviewIdAndUserId(12L, 3L)).thenReturn(true);

        BusinessExcepion e = assertThrows(BusinessExcepion.class,
                () -> service.report(12L, new ReviewReportRequestDTO(ReportReason.SPAM, null), reporter));
        assertEquals("Você já denunciou esta avaliação", e.getMessage());
    }

    @Test
    void report_concurrentDuplicate_is409() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

        assertThrows(BusinessExcepion.class,
                () -> service.report(12L, new ReviewReportRequestDTO(ReportReason.SPAM, null), reporter));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void report_hiddenOrMissing_is404() {
        review.setStatus(ReviewStatus.HIDDEN);
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.report(12L, new ReviewReportRequestDTO(ReportReason.SPAM, null), reporter));
        assertThrows(NotFoundException.class,
                () -> service.report(99L, new ReviewReportRequestDTO(ReportReason.SPAM, null), reporter));
    }

    @Test
    void listReports_mapsViews() {
        when(reviewRepository.existsById(12L)).thenReturn(true);
        ReviewReportRepository.ReportView view = mock(ReviewReportRepository.ReportView.class);
        when(view.getId()).thenReturn(5L);
        when(view.getReason()).thenReturn(ReportReason.OFFENSIVE);
        when(view.getDetails()).thenReturn("x");
        when(view.getReporterName()).thenReturn("Leitor  ");
        when(view.getCreatedAt()).thenReturn(Instant.parse("2026-10-02T10:00:00Z"));
        when(reviewReportRepository.findViewsByReview(12L)).thenReturn(List.of(view));

        List<ReviewReportDTO> reports = service.listReports(12L);

        assertEquals(1, reports.size());
        assertEquals("Leitor", reports.get(0).getReporterName());
        assertEquals(ReportReason.OFFENSIVE, reports.get(0).getReason());
    }

    @Test
    void listReports_missingReview_is404() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.listReports(99L));
    }

    @Test
    void dismissReports_deletesAndPublishesOnlyWhenSomethingWasRemoved() {
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.deleteByReview(12L)).thenReturn(2, 0);

        service.dismissReports(12L, admin);
        service.dismissReports(12L, admin);

        verify(eventPublisher, times(1)).publish(eq(EventType.REVIEW_REPORTS_DISMISSED), eq(12L),
                contains("descartou 2 denúncias"), eq(admin));
    }

    @Test
    void dismissReports_missingReview_is404() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.dismissReports(99L, admin));
    }
}
