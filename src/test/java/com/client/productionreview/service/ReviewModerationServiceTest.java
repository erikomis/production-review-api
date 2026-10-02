package com.client.productionreview.service;

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
import com.client.productionreview.service.impl.ReviewModerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewModerationServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private com.client.productionreview.repositories.jpa.ReviewReportRepository reviewReportRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.client.productionreview.service.impl.ReviewEnricher reviewEnricher;

    @InjectMocks
    private ReviewModerationServiceImpl service;

    private final User admin = User.builder().id(1L).name("Administrador").build();
    private Review review;

    @BeforeEach
    void setUp() {
        review = Review.builder().id(7L).title("Spam").description("compre já").note(1L).productId(2L).userId(3L)
                .status(ReviewStatus.VISIBLE).build();
    }

    private void stubDetails() {
        when(reviewRepository.searchDetails(any(ReviewSearch.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(ReviewResponseDTO.builder().id(7L).status(review.getStatus())
                        .moderationReason(review.getModerationReason()).moderatedByName("Administrador").build())));
    }

    @Test
    void hide_requiresReason() {
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.HIDDEN, null), admin));
        assertThrows(BadRequestException.class,
                () -> service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.HIDDEN, "   "), admin));

        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void hide_setsStatusReasonAndModerator_andPublishesEvent() {
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        stubDetails();

        ReviewResponseDTO dto = service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.HIDDEN, " spam "), admin);

        assertEquals(ReviewStatus.HIDDEN, review.getStatus());
        assertEquals("spam", review.getModerationReason());
        assertEquals(1L, review.getModeratedBy());
        assertNotNull(review.getModeratedAt());
        verify(reviewRepository).save(review);
        verify(eventPublisher).publish(eq(EventType.REVIEW_HIDDEN), eq(7L), contains("spam"), eq(admin));

        assertEquals(ReviewStatus.HIDDEN, dto.getStatus());
        assertEquals("Administrador", dto.getModeratedByName());
    }

    @Test
    void restore_doesNotRequireReason() {
        review.setStatus(ReviewStatus.HIDDEN);
        review.setModerationReason("spam");
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        stubDetails();

        service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.VISIBLE, null), admin);

        assertEquals(ReviewStatus.VISIBLE, review.getStatus());
        assertNull(review.getModerationReason());
        verify(eventPublisher).publish(eq(EventType.REVIEW_RESTORED), eq(7L), anyString(), eq(admin));
    }

    @Test
    void moderate_missingReview_isNotFound() {
        when(reviewRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.VISIBLE, null), admin));
    }

    @Test
    void list_passesFiltersNewestFirst() {
        var pageable = PageRequest.of(0, 10);
        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        when(reviewRepository.searchDetails(search.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.listReviews(ReviewStatus.HIDDEN, 1L, 2L, "spam", pageable);

        assertEquals(ReviewStatus.HIDDEN, search.getValue().status());
        assertEquals(1L, search.getValue().note());
        assertEquals(2L, search.getValue().productId());
        assertEquals("spam", search.getValue().search());
        assertEquals(ReviewSort.recent, search.getValue().sort());
    }

    @Test
    void list_invalidNote_isBadRequest() {
        assertThrows(BadRequestException.class, () -> service.listReviews(null, 9L, null, null, PageRequest.of(0, 10)));
    }

    // ---------- fase 3: denúncias, notificações, lote e resposta oficial ----------

    @Test
    void hide_resolvesReportsAndNotifiesAuthor() {
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        stubDetails();

        service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.HIDDEN, "spam"), admin);

        verify(reviewReportRepository).deleteByReview(7L);
        verify(notificationService).reviewHidden(review, "spam");
        verify(reviewEnricher).enrich(any(), eq(true));
    }

    @Test
    void restore_notifiesAuthor_butNotWhenStatusDoesNotChange() {
        review.setStatus(ReviewStatus.HIDDEN);
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        stubDetails();

        service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.VISIBLE, null), admin);
        service.moderate(7L, new ReviewModerationRequestDTO(ReviewStatus.VISIBLE, null), admin);

        verify(notificationService, times(1)).reviewRestored(review);
        verify(reviewReportRepository, never()).deleteByReview(any());
    }

    @Test
    void list_reportedFilter_andReportsCount() {
        var pageable = PageRequest.of(0, 10);
        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        when(reviewRepository.searchDetails(search.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.listReviews(null, null, null, null, true, pageable);
        service.listReviews(null, null, null, null, false, pageable);

        assertEquals(Boolean.TRUE, search.getAllValues().get(0).reported());
        // reported=false não filtra (mostra todas)
        assertNull(search.getAllValues().get(1).reported());
        verify(reviewEnricher, times(2)).enrich(any(), eq(true));
    }

    @Test
    void bulk_hidesAllFoundReviews_withSameRules() {
        Review other = Review.builder().id(8L).title("Outra").userId(4L).status(ReviewStatus.VISIBLE).build();
        when(reviewRepository.findAllById(List.of(7L, 8L, 99L))).thenReturn(List.of(review, other));

        int updated = service.moderateBulk(com.client.productionreview.dtos.review.ReviewBulkModerationRequestDTO.builder()
                .ids(List.of(7L, 8L, 7L, 99L)).status(ReviewStatus.HIDDEN).reason(" spam ").build(), admin);

        assertEquals(2, updated);
        assertEquals(ReviewStatus.HIDDEN, other.getStatus());
        assertEquals("spam", other.getModerationReason());
        verify(reviewReportRepository).deleteByReview(7L);
        verify(reviewReportRepository).deleteByReview(8L);
        verify(notificationService).reviewHidden(review, "spam");
        verify(notificationService).reviewHidden(other, "spam");
        verify(eventPublisher).publish(eq(EventType.REVIEWS_BULK_MODERATED), isNull(),
                eq("Administrador ocultou 2 avaliações em lote (7, 8): spam"), eq(admin));
        verify(eventPublisher, never()).publish(eq(EventType.REVIEW_HIDDEN), any(), anyString(), any());
    }

    @Test
    void bulk_hideWithoutReason_is400() {
        assertThrows(BadRequestException.class, () -> service.moderateBulk(
                com.client.productionreview.dtos.review.ReviewBulkModerationRequestDTO.builder()
                        .ids(List.of(7L)).status(ReviewStatus.HIDDEN).build(), admin));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void bulk_nothingFound_returnsZeroWithoutEvent() {
        when(reviewRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertEquals(0, service.moderateBulk(com.client.productionreview.dtos.review.ReviewBulkModerationRequestDTO.builder()
                .ids(List.of(99L)).status(ReviewStatus.VISIBLE).build(), admin));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void reply_savesNotifiesAndPublishes() {
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        stubDetails();

        service.reply(7L, "  Obrigado pelo retorno!  ", admin);

        assertEquals("Obrigado pelo retorno!", review.getReplyText());
        assertEquals(1L, review.getReplyAuthorId());
        assertNotNull(review.getRepliedAt());
        verify(reviewRepository).save(review);
        verify(notificationService).reviewReplied(review, "Obrigado pelo retorno!");
        verify(eventPublisher).publish(eq(EventType.REVIEW_REPLIED), eq(7L), contains("respondeu"), eq(admin));
    }

    @Test
    void reply_editingKeepsSingleReply() {
        review.setReplyText("antiga");
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        stubDetails();

        service.reply(7L, "nova", admin);

        assertEquals("nova", review.getReplyText());
        verify(eventPublisher).publish(eq(EventType.REVIEW_REPLIED), eq(7L), contains("editou a resposta"), eq(admin));
    }

    @Test
    void reply_missingReview_is404() {
        when(reviewRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.reply(7L, "x", admin));
        verifyNoInteractions(notificationService);
    }

    @Test
    void deleteReply_clearsFieldsAndPublishes_idempotent() {
        review.setReplyText("antiga");
        review.setReplyAuthorId(1L);
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));

        service.deleteReply(7L, admin);
        service.deleteReply(7L, admin);

        assertNull(review.getReplyText());
        assertNull(review.getReplyAuthorId());
        verify(reviewRepository, times(1)).save(review);
        verify(eventPublisher, times(1)).publish(eq(EventType.REVIEW_REPLY_DELETED), eq(7L), anyString(), eq(admin));
    }
}
