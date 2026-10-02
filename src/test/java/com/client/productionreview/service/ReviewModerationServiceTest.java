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
}
