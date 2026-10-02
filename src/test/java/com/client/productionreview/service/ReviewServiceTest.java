package com.client.productionreview.service;


import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;

import com.client.productionreview.dtos.review.HelpfulResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.ReviewHelpful;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.repositories.jpa.ReviewHelpfulRepository;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.impl.ReviewServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository  reviewRepository;

    @Mock
    private ReviewHelpfulRepository reviewHelpfulRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    // dependências novas (fase 3): fotos, denúncias, notificações e perfil
    @Mock
    private com.client.productionreview.service.impl.ReviewEnricher reviewEnricher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReviewImageService reviewImageService;

    @Mock
    private com.client.productionreview.repositories.jpa.ReviewReportRepository reviewReportRepository;

    @Mock
    private com.client.productionreview.repositories.jpa.UserRepository userRepository;

    private Review review;
    private Product product;
    private User owner;
    private User otherUser;
    private User admin;
    private Long reviewId;
    private Long productId;

    @BeforeEach
    public void setUp() {
        reviewId = 1L;
        productId = 2L;
        product = new Product();
        product.setId(productId);
        product.setName("product");
        review = new Review();
        review.setProductId(productId);
        review.setTitle("title");
        review.setDescription("description");
        review.setNote(5L);

        owner = User.builder().id(10L).name("owner").build();
        otherUser = User.builder().id(20L).name("other").build();
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        admin = User.builder().id(30L).name("admin").roles(List.of(adminRole)).build();
    }

    private Review existingReview() {
        Review existing = new Review();
        existing.setId(reviewId);
        existing.setUserId(owner.getId());
        existing.setProductId(productId);
        existing.setTitle("old title");
        return existing;
    }


    @Test
    public void testSaveReview_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.saveReview(review, "nameUser"));
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testSaveReview_Success() {
        review.setUserId(owner.getId());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenAnswer(inv -> {
            review.setId(12L);
            return review;
        });

        Review savedReview = reviewService.saveReview(review, "nameUser");

        assertNotNull(savedReview);
        assertEquals(ReviewStatus.VISIBLE, savedReview.getStatus());
        verify(reviewRepository).save(review);

        ArgumentCaptor<User> actor = ArgumentCaptor.forClass(User.class);
        verify(eventPublisher).publish(eq(EventType.REVIEW_CREATED), eq(12L),
                eq("nameUser avaliou product com 5 estrelas"), actor.capture());
        assertEquals("nameUser", actor.getValue().getName());
        assertEquals(owner.getId(), actor.getValue().getId());
    }

    @Test
    public void testSaveReview_PublisherFailureDoesNotBreakCreation() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenReturn(review);
        // o publicador real nunca lança; quando falha devolve null
        when(eventPublisher.publish(any(), any(), anyString(), any())).thenReturn(null);

        assertNotNull(reviewService.saveReview(review, "nameUser"));
    }

    @Test
    public void testUpdateReview_KeepsModerationStatusAndPublishesEvent() {
        Review existing = existingReview();
        existing.setStatus(ReviewStatus.HIDDEN);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review updated = reviewService.updateReview(review, reviewId, owner);

        assertEquals(ReviewStatus.HIDDEN, updated.getStatus());
        verify(eventPublisher).publish(eq(EventType.REVIEW_UPDATED), eq(reviewId), anyString());
    }

    @Test
    public void testCommentStream_SurvivesSubscriberCancel() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any())).thenReturn(review);

        // primeiro assinante entra e sai
        StepVerifier.create(reviewService.getCommentStream())
                .then(() -> reviewService.saveReview(review, "first"))
                .assertNext(n -> assertEquals("first", n.getNameUser()))
                .thenCancel()
                .verify();

        // o stream deve continuar funcionando para novos assinantes
        StepVerifier.create(reviewService.getCommentStream())
                .then(() -> reviewService.saveReview(review, "second"))
                .assertNext(n -> assertEquals("second", n.getNameUser()))
                .thenCancel()
                .verify();
    }

    @Test
    public void testUpdateReview_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.updateReview(review, reviewId, owner));
    }

    @Test
    public void testUpdateReview_ReviewNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.updateReview(review, reviewId, owner));
    }

    @Test
    public void testUpdateReview_UpdatesExistingRecordAndKeepsOwner() {
        Review existing = existingReview();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // o mapper monta a review sem id; o service deve atualizar a existente
        Review updatedReview = reviewService.updateReview(review, reviewId, owner);

        assertEquals(reviewId, updatedReview.getId());
        assertEquals(owner.getId(), updatedReview.getUserId());
        assertEquals("title", updatedReview.getTitle());
        assertEquals(5L, updatedReview.getNote());
    }

    @Test
    public void testUpdateReview_ForbiddenForOtherUser() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        GlobalException ex = assertThrows(GlobalException.class, () -> reviewService.updateReview(review, reviewId, otherUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    public void testUpdateReview_AllowedForAdmin() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review updated = reviewService.updateReview(review, reviewId, admin);

        assertEquals(owner.getId(), updated.getUserId());
    }

    @Test
    public void testDeleteReview_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.deleteReview(reviewId, owner));
    }

    @Test
    public void testDeleteReview_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        reviewService.deleteReview(reviewId, owner);

        verify(reviewHelpfulRepository).deleteByReview(reviewId);
        verify(reviewRepository).deleteById(reviewId);
        verify(eventPublisher).publish(eq(EventType.REVIEW_DELETED), eq(reviewId), anyString());
    }

    @Test
    public void testDeleteReview_ForbiddenForOtherUser() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        assertThrows(GlobalException.class, () -> reviewService.deleteReview(reviewId, otherUser));

        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    public void testGetReview_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.getReview(reviewId));
    }

    @Test
    public void testGetReview_Success() {

        review.setId(1L);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        Review fetchedReview = reviewService.getReview(reviewId);

        assertNotNull(fetchedReview);
        assertEquals(reviewId, fetchedReview.getId());
    }

    @Test
    public void testGetReviews_onlyVisibleNewestFirst() {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        var dto = ReviewResponseDTO.builder().id(1L).productName("product").userName("owner").build();
        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        when(reviewRepository.searchDetails(search.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of(dto)));

        Page<ReviewResponseDTO> reviews = reviewService.getReviews(pageable);

        assertEquals(1, reviews.getTotalElements());
        assertEquals("owner", reviews.getContent().get(0).getUserName());
        assertEquals(ReviewStatus.VISIBLE, search.getValue().status());
        assertEquals(ReviewSort.recent, search.getValue().sort());
        // sem login, helpfulByMe fica false e nem consulta as marcações
        assertFalse(reviews.getContent().get(0).isHelpfulByMe());
        verifyNoInteractions(reviewHelpfulRepository);
    }

    @Test
    public void testGetReviewsByProduct_filtersAndFillsHelpfulByMe() {
        var pageable = PageRequest.of(0, 10);
        authenticate(otherUser);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        when(reviewRepository.searchDetails(search.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of(
                ReviewResponseDTO.builder().id(1L).build(), ReviewResponseDTO.builder().id(2L).build())));
        when(reviewHelpfulRepository.findReviewIdsMarkedBy(otherUser.getId(), List.of(1L, 2L))).thenReturn(List.of(2L));

        Page<ReviewResponseDTO> page = reviewService.getReviewsByProduct(productId, 4L, "helpful", pageable);

        assertEquals(2, page.getTotalElements());
        assertFalse(page.getContent().get(0).isHelpfulByMe());
        assertTrue(page.getContent().get(1).isHelpfulByMe());
        assertEquals(productId, search.getValue().productId());
        assertEquals(4L, search.getValue().note());
        assertEquals(ReviewStatus.VISIBLE, search.getValue().status());
        assertEquals(ReviewSort.helpful, search.getValue().sort());
    }

    @Test
    public void testGetReviewsByProduct_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.getReviewsByProduct(productId, null, null, PageRequest.of(0, 10)));
    }

    @Test
    public void testGetReviewsByProduct_invalidSortOrNote() {
        assertThrows(BadRequestException.class, () -> reviewService.getReviewsByProduct(productId, null, "random", PageRequest.of(0, 10)));
        assertThrows(BadRequestException.class, () -> reviewService.getReviewsByProduct(productId, 6L, null, PageRequest.of(0, 10)));
        verifyNoInteractions(reviewRepository);
    }

    @Test
    public void testGetMyReviews_includesHiddenOfTheUser() {
        var pageable = PageRequest.of(0, 10);
        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        when(reviewRepository.searchDetails(search.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of(
                ReviewResponseDTO.builder().id(1L).status(ReviewStatus.HIDDEN).moderationReason("spam").build())));

        Page<ReviewResponseDTO> page = reviewService.getMyReviews(owner.getId(), pageable);

        assertEquals(ReviewStatus.HIDDEN, page.getContent().get(0).getStatus());
        assertEquals(owner.getId(), search.getValue().userId());
        assertNull(search.getValue().status());
    }

    @Test
    public void testGetReviewDetails_hiddenIsNotFoundForOthers() {
        when(reviewRepository.searchDetails(any(), any())).thenReturn(new PageImpl<>(List.of(
                ReviewResponseDTO.builder().id(1L).userId(owner.getId()).status(ReviewStatus.HIDDEN).build())));

        assertThrows(NotFoundException.class, () -> reviewService.getReviewDetails(1L));

        authenticate(otherUser);
        assertThrows(NotFoundException.class, () -> reviewService.getReviewDetails(1L));

        authenticate(owner);
        assertEquals(1L, reviewService.getReviewDetails(1L).getId());

        authenticate(admin);
        assertEquals(1L, reviewService.getReviewDetails(1L).getId());
    }

    // ---------- útil ----------

    @Test
    public void testToggleHelpful_marksAndUnmarks() {
        Review existing = existingReview();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));
        when(reviewHelpfulRepository.existsByReviewIdAndUserId(reviewId, otherUser.getId())).thenReturn(false, true);
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(1L, 0L);

        HelpfulResponseDTO marked = reviewService.toggleHelpful(reviewId, otherUser);

        assertTrue(marked.isHelpfulByMe());
        assertEquals(1L, marked.getHelpfulCount());
        assertEquals(reviewId, marked.getReviewId());
        verify(reviewHelpfulRepository).saveAndFlush(any(ReviewHelpful.class));

        HelpfulResponseDTO unmarked = reviewService.toggleHelpful(reviewId, otherUser);

        assertFalse(unmarked.isHelpfulByMe());
        assertEquals(0L, unmarked.getHelpfulCount());
        verify(reviewHelpfulRepository).deleteMark(reviewId, otherUser.getId());
    }

    @Test
    public void testToggleHelpful_concurrentDoubleClickStaysMarked() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));
        when(reviewHelpfulRepository.existsByReviewIdAndUserId(reviewId, otherUser.getId())).thenReturn(false);
        // a outra requisição gravou a marcação entre o exists e o insert
        when(reviewHelpfulRepository.saveAndFlush(any(ReviewHelpful.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(1L);

        HelpfulResponseDTO result = reviewService.toggleHelpful(reviewId, otherUser);

        assertTrue(result.isHelpfulByMe());
        assertEquals(1L, result.getHelpfulCount());
    }

    @Test
    public void testToggleHelpful_ownReviewIsBadRequest() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        assertThrows(BadRequestException.class, () -> reviewService.toggleHelpful(reviewId, owner));
        verify(reviewHelpfulRepository, never()).save(any());
    }

    @Test
    public void testToggleHelpful_hiddenOrMissingIsNotFound() {
        Review hidden = existingReview();
        hidden.setStatus(ReviewStatus.HIDDEN);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(hidden));
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.toggleHelpful(reviewId, otherUser));
        assertThrows(NotFoundException.class, () -> reviewService.toggleHelpful(99L, otherUser));
        verifyNoInteractions(reviewHelpfulRepository);
    }

    @Test
    public void testGetProductSummary_distributionHasAllKeys() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.getRatingSummary(productId)).thenReturn(summary(3L, 4.3333));
        when(reviewRepository.countByNoteForProduct(productId)).thenReturn(List.of(noteCount(5L, 2L), noteCount(3L, 1L)));

        ReviewSummaryDTO summary = reviewService.getProductSummary(productId);

        assertEquals(List.of("1", "2", "3", "4", "5"), List.copyOf(summary.getDistribution().keySet()));
        assertEquals(0L, summary.getDistribution().get("1"));
        assertEquals(1L, summary.getDistribution().get("3"));
        assertEquals(2L, summary.getDistribution().get("5"));
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private ReviewRepository.NoteCount noteCount(Long note, Long total) {
        return new ReviewRepository.NoteCount() {
            @Override
            public Long getNote() {
                return note;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    @Test
    public void testGetProductSummary_RoundsAverage() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.getRatingSummary(productId)).thenReturn(summary(3L, 4.3333));

        ReviewSummaryDTO summary = reviewService.getProductSummary(productId);

        assertEquals(productId, summary.getProductId());
        assertEquals(3L, summary.getTotalReviews());
        assertEquals(4.3, summary.getAverageNote());
    }

    @Test
    public void testGetProductSummary_NoReviews() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.getRatingSummary(productId)).thenReturn(summary(0L, null));

        ReviewSummaryDTO summary = reviewService.getProductSummary(productId);

        assertEquals(0L, summary.getTotalReviews());
        assertEquals(0.0, summary.getAverageNote());
    }

    private ReviewRepository.RatingSummary summary(Long total, Double average) {
        return new ReviewRepository.RatingSummary() {
            @Override
            public Long getTotalReviews() {
                return total;
            }

            @Override
            public Double getAverageNote() {
                return average;
            }
        };
    }


    // ---------- fase 3 ----------

    @Test
    public void testSaveReview_notifiesFollowers() {
        review.setUserId(owner.getId());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenReturn(review);

        reviewService.saveReview(review, "nameUser");

        verify(notificationService).followedProductReview(review, product, "nameUser");
    }

    @Test
    public void testToggleHelpful_markNotifiesAuthor_unmarkDoesNot() {
        Review existing = existingReview();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));
        when(reviewHelpfulRepository.existsByReviewIdAndUserId(reviewId, otherUser.getId())).thenReturn(false, true);
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(3L, 2L);

        reviewService.toggleHelpful(reviewId, otherUser);
        reviewService.toggleHelpful(reviewId, otherUser);

        verify(notificationService, times(1)).reviewHelpful(existing, 3L);
    }

    @Test
    public void testDeleteReview_removesImagesAndReports() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview()));

        reviewService.deleteReview(reviewId, owner);

        verify(reviewReportRepository).deleteByReview(reviewId);
        verify(reviewImageService).deleteAllForReview(reviewId);
        verify(reviewRepository).deleteById(reviewId);
    }

    @Test
    public void testGetUserReviews_onlyVisibleOfActiveUser() {
        User maria = User.builder().id(30L).username("maria").active(true).build();
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(maria));
        when(reviewRepository.searchDetails(any(), any())).thenReturn(new PageImpl<>(List.of(
                ReviewResponseDTO.builder().id(5L).build())));

        assertEquals(1, reviewService.getUserReviews("maria", PageRequest.of(0, 10)).getTotalElements());

        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        verify(reviewRepository).searchDetails(search.capture(), any());
        assertEquals(30L, search.getValue().userId());
        assertEquals(ReviewStatus.VISIBLE, search.getValue().status());
        verify(reviewEnricher).enrich(any(), eq(false));
    }

    @Test
    public void testGetUserReviews_inactiveOrMissingIs404() {
        when(userRepository.findByUsername("inativo"))
                .thenReturn(Optional.of(User.builder().id(31L).username("inativo").active(false).build()));
        when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.getUserReviews("inativo", PageRequest.of(0, 10)));
        assertThrows(NotFoundException.class, () -> reviewService.getUserReviews("ninguem", PageRequest.of(0, 10)));
    }
}
